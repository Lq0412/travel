package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.AI.core.service.ImageGenerationService;
import com.lq.travel.constant.AiMqConstants;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.manager.upload.UrlPictureUpload;
import com.lq.travel.mapper.MemoryCardHistoryMapper;
import com.lq.travel.mapper.MemoryCardMapper;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.dto.trip.MemoryCardGenerateResponse;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.model.entity.MemoryCard;
import com.lq.travel.model.entity.MemoryCardHistory;
import com.lq.travel.model.entity.Trip;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;
import com.lq.travel.mq.TaskMessageProducer;
import com.lq.travel.service.AiTaskService;
import com.lq.travel.service.MemoryCardService;
import com.lq.travel.service.TripPhotoService;
import com.lq.travel.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 回忆图服务实现类
 */
@Slf4j
@Service
public class MemoryCardServiceImpl extends ServiceImpl<MemoryCardMapper, MemoryCard> implements MemoryCardService {

    @Resource
    private TripService tripService;

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private AiTaskService aiTaskService;

    @Autowired(required = false) // 可选注入，未配置 MQ 时允许为空
    private TaskMessageProducer taskMessageProducer;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ai.mq.memory-card-enabled:false}")
    private boolean memoryCardMqEnabled;

    @Value("${ai.mq.retry.max-retry:3}")
    private int aiTaskMaxRetry;

    
    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private MemoryCardHistoryMapper memoryCardHistoryMapper;

    @Resource
    private TripPhotoService tripPhotoService;

    @Override
    @Transactional
    public MemoryCardGenerateResponse generateMemoryCard(MemoryCardGenerateRequest request, User user) {
        if (request.getTripId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "行程ID不能为空");
        }
        if (request.getPhotoUrls() == null || request.getPhotoUrls().size() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要3张照片");
        }
        if (request.getPhotoUrls().size() > 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多只能使用6张照片");
        }

        Trip trip = tripService.getById(request.getTripId());
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }

        QueryWrapper<MemoryCard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", request.getTripId());
        queryWrapper.eq("is_delete", 0);
        MemoryCard existingCard = this.getOne(queryWrapper);
        if (existingCard != null && "success".equals(existingCard.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该行程已生成回忆图");
        }

        MemoryCard memoryCard = existingCard != null ? existingCard : new MemoryCard();
        if (existingCard == null) {
            memoryCard.setTripId(request.getTripId());
            memoryCard.setUserId(user.getId());
            memoryCard.setTemplateName(request.getTemplateName() != null ? request.getTemplateName() : "default");
        }
        memoryCard.setStatus("pending");
        memoryCard.setRetryCount(0);
        memoryCard.setErrorMessage(null);
        memoryCard.setRemoteTaskId(null);

        String prompt = buildImagePrompt(trip, request.getPhotoUrls());

        // MQ 异步方案（需配置开启），否则走原同步调用
        if (memoryCardMqEnabled && taskMessageProducer != null) {
            String taskId = UUID.randomUUID().toString();
            String bizKey = buildBizKey(user.getId(), request.getTripId(), request.getPhotoUrls());
            Map<String, Object> payload = buildTaskPayload(prompt, request, user);
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务序列化失败: " + e.getMessage());
            }

            AiTask task = aiTaskService.createTask(taskId, bizKey, AiMqConstants.ROUTING_MEMORY_CARD,
                    payloadJson, user.getId(), request.getTripId(), aiTaskMaxRetry);
            log.info("Create MQ memory-card task: taskId={}, bizKey={}, status={}", task.getTaskId(), task.getBizKey(), task.getStatus());

            memoryCard.setTaskId(taskId);
            this.saveOrUpdate(memoryCard);

            payload.put("taskId", taskId);
            payload.put("bizKey", bizKey);
            payload.put("type", AiMqConstants.ROUTING_MEMORY_CARD);
            taskMessageProducer.sendMemoryCardTask(payload);

            MemoryCardGenerateResponse response = new MemoryCardGenerateResponse();
            response.setTaskId(taskId);
            response.setStatus("pending");
            return response;
        }

        ImageGenerationService.ImageGenerationRequest imageRequest = new ImageGenerationService.ImageGenerationRequest();
        imageRequest.setPrompt(prompt);
        imageRequest.setReferenceImageUrls(request.getPhotoUrls());
        imageRequest.setSize("1328x1328");
        imageRequest.setStyle("fresh");
        imageRequest.setQuality("standard");

        try {
            String taskId = imageGenerationService.generateImageAsync(imageRequest);
            memoryCard.setTaskId(taskId);
            memoryCard.setRemoteTaskId(taskId);
            this.saveOrUpdate(memoryCard);

            MemoryCardGenerateResponse response = new MemoryCardGenerateResponse();
            response.setTaskId(taskId);
            response.setStatus("pending");
            return response;
        } catch (Exception e) {
            log.error("生成回忆图失败", e);
            memoryCard.setStatus("failed");
            memoryCard.setErrorMessage("生成失败: " + e.getMessage());
            this.saveOrUpdate(memoryCard);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成回忆图失败: " + e.getMessage());
        }
    }

    private String buildBizKey(Long userId, Long tripId, List<String> photoUrls) {
        int photosHash = photoUrls != null ? photoUrls.hashCode() : 0;
        return userId + ":" + tripId + ":" + photosHash;
    }

    private Map<String, Object> buildTaskPayload(String prompt, MemoryCardGenerateRequest request, User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("photoUrls", request.getPhotoUrls());
        payload.put("templateName", request.getTemplateName());
        payload.put("tripId", request.getTripId());
        payload.put("userId", user.getId());
        payload.put("size", "1328x1328");
        payload.put("style", "fresh");
        payload.put("quality", "standard");
        return payload;
    }

    /**
     * 构建图片生成提示词
     */
    private String buildImagePrompt(Trip trip, List<String> photoUrls) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("生成一张旅行回忆卡海报，包含以下要素：\n");
        prompt.append("目的地：").append(trip.getDestination()).append("\n");
        prompt.append("天数：").append(trip.getDays()).append("天\n");
        if (trip.getTheme() != null) {
            prompt.append("主题：").append(trip.getTheme()).append("\n");
        }
        prompt.append("风格：清新/明亮，具有回忆感\n");
        prompt.append("参考照片：").append(photoUrls.size()).append(" 张旅行照片\n");
        prompt.append("要求：将照片元素融合到海报设计，整体排版统一，适合做旅途回忆展示");
        return prompt.toString();
    }

    @Override
    public MemoryCardVO getMemoryCardStatus(String taskId, Long userId) {
        Optional<AiTask> aiTaskOpt = Optional.empty();
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("task_id", taskId)
                .eq("is_delete", 0));

        if (memoryCard == null) {
            aiTaskOpt = aiTaskService.findByTaskId(taskId);
            if (aiTaskOpt.isPresent() && aiTaskOpt.get().getTripId() != null) {
                memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                        .eq("trip_id", aiTaskOpt.get().getTripId())
                        .eq("is_delete", 0)
                        .last("limit 1"));
            }
        }

        if (memoryCard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回忆图不存在");
        }

        if (!memoryCard.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该回忆图");
        }

        if (aiTaskOpt.isEmpty()) {
            aiTaskOpt = aiTaskService.findByTaskId(memoryCard.getTaskId());
        }

        String remoteTaskId = resolveRemoteTaskId(memoryCard, aiTaskOpt);
        if (!StringUtils.hasText(remoteTaskId)) {
            return convertToVO(memoryCard);
        }

        ImageGenerationService.ImageGenerationResult taskResult =
                imageGenerationService.getTaskStatus(remoteTaskId);

        if (!memoryCard.getStatus().equals(taskResult.getStatus())) {
            memoryCard.setStatus(taskResult.getStatus());
            memoryCard.setRemoteTaskId(remoteTaskId);
            if ("success".equals(taskResult.getStatus()) && taskResult.getImageUrl() != null) {
                try {
                    UploadPictureResult uploadResult = urlPictureUpload.uploadPicture(taskResult.getImageUrl(), "memory-card/" + memoryCard.getTripId());
                    String cosUrl = uploadResult.getUrl();
                    saveHistory(memoryCard, cosUrl);
                    memoryCard.setImageUrl(cosUrl);
                    markAiTaskSuccess(memoryCard, remoteTaskId, cosUrl);
                } catch (Exception ex) {
                    String fallbackUrl = taskResult.getImageUrl();
                    saveHistory(memoryCard, fallbackUrl);
                    memoryCard.setImageUrl(fallbackUrl);
                    markAiTaskSuccess(memoryCard, remoteTaskId, fallbackUrl);
                }
            }
            if ("failed".equals(taskResult.getStatus())) {
                if (taskResult.getErrorMessage() != null) {
                    memoryCard.setErrorMessage(taskResult.getErrorMessage());
                }
                markAiTaskFailed(memoryCard, memoryCard.getErrorMessage());
            } else {
                memoryCard.setErrorMessage(null);
                if ("processing".equals(taskResult.getStatus())) {
                    markAiTaskProcessing(memoryCard, remoteTaskId);
                }
            }
            this.updateById(memoryCard);
        }

        return convertToVO(memoryCard);
    }

    /**
     * 重试生成（占位，暂不自动触发）
     */
    private void retryGeneration(MemoryCard memoryCard) {
        log.info("自动重试生成回忆图，任务ID: {}", memoryCard.getTaskId());
        memoryCard.setRetryCount(memoryCard.getRetryCount() + 1);
        memoryCard.setStatus("pending");
        memoryCard.setErrorMessage(null);
        memoryCard.setRemoteTaskId(null);
    }

    @Override
    public MemoryCardVO getMemoryCardByTripId(Long tripId, Long userId) {
        QueryWrapper<MemoryCard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", tripId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT 1");

        MemoryCard memoryCard = this.getOne(queryWrapper);

        if (memoryCard == null) {
            return null;
        }

        if (!memoryCard.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该回忆图");
        }

        return convertToVO(memoryCard);
    }

    /**
     * 定时任务：轮询并更新回忆图生成状态
     */
    @Scheduled(fixedDelay = 30000) // 每 30 秒执行一次
    public void updateMemoryCardStatus() {
        try {
            QueryWrapper<MemoryCard> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("status", "pending", "processing");
            queryWrapper.eq("is_delete", 0);

            List<MemoryCard> memoryCards = this.list(queryWrapper);

            for (MemoryCard memoryCard : memoryCards) {
                try {
                    if (memoryCard.getTaskId() != null) {
                        Optional<AiTask> aiTaskOpt = aiTaskService.findByTaskId(memoryCard.getTaskId());
                        String remoteTaskId = resolveRemoteTaskId(memoryCard, aiTaskOpt);
                        if (!StringUtils.hasText(remoteTaskId)) {
                            continue;
                        }
                        ImageGenerationService.ImageGenerationResult taskResult =
                                imageGenerationService.getTaskStatus(remoteTaskId);

                        if (!memoryCard.getStatus().equals(taskResult.getStatus())) {
                            memoryCard.setStatus(taskResult.getStatus());
                            memoryCard.setRemoteTaskId(remoteTaskId);
                            if ("success".equals(taskResult.getStatus()) && taskResult.getImageUrl() != null) {
                                try {
                                    UploadPictureResult uploadResult = urlPictureUpload.uploadPicture(taskResult.getImageUrl(), "memory-card/" + memoryCard.getTripId());
                                    String cosUrl = uploadResult.getUrl();
                                    saveHistory(memoryCard, cosUrl);
                                    memoryCard.setImageUrl(cosUrl);
                                    markAiTaskSuccess(memoryCard, remoteTaskId, cosUrl);
                                } catch (Exception ex) {
                                    String fallbackUrl = taskResult.getImageUrl();
                                    saveHistory(memoryCard, fallbackUrl);
                                    memoryCard.setImageUrl(fallbackUrl);
                                    markAiTaskSuccess(memoryCard, remoteTaskId, fallbackUrl);
                                }
                            }
                            if ("failed".equals(taskResult.getStatus())) {
                                if (taskResult.getErrorMessage() != null) {
                                    memoryCard.setErrorMessage(taskResult.getErrorMessage());
                                }
                                markAiTaskFailed(memoryCard, memoryCard.getErrorMessage());
                            } else {
                                memoryCard.setErrorMessage(null);
                                if ("processing".equals(taskResult.getStatus())) {
                                    markAiTaskProcessing(memoryCard, remoteTaskId);
                                }
                            }
                            this.updateById(memoryCard);
                        }
                    }
                } catch (Exception e) {
                    log.error("更新回忆图状态失败，ID: {}", memoryCard.getId(), e);
                }
            }
        } catch (BadSqlGrammarException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause != null && rootCause.getMessage() != null && rootCause.getMessage().contains("doesn't exist")) {
                log.debug("回忆图表尚未创建，跳过本次定时任务");
            } else {
                log.error("定时任务执行失败：数据库查询异常", e);
            }
        } catch (DataAccessException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause != null && rootCause.getMessage() != null && rootCause.getMessage().contains("doesn't exist")) {
                log.debug("回忆图表尚未创建，跳过本次定时任务");
            } else {
                log.error("定时任务执行失败：数据库访问异常", e);
            }
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    private String resolveRemoteTaskId(MemoryCard memoryCard, Optional<AiTask> aiTaskOpt) {
        if (memoryCard != null && StringUtils.hasText(memoryCard.getRemoteTaskId())) {
            return memoryCard.getRemoteTaskId();
        }
        if (aiTaskOpt != null && aiTaskOpt.isPresent()) {
            String remoteFromResult = extractRemoteTaskId(aiTaskOpt.get().getResult());
            if (StringUtils.hasText(remoteFromResult)) {
                memoryCard.setRemoteTaskId(remoteFromResult);
                this.updateById(memoryCard);
                return remoteFromResult;
            }
        }
        if (memoryCardMqEnabled) {
            return null;
        }
        return memoryCard != null ? memoryCard.getTaskId() : null;
    }

    private String extractRemoteTaskId(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            JsonNode remote = node.path("remoteTaskId");
            if (!remote.isMissingNode() && !remote.isNull()) {
                String val = remote.asText(null);
                return StringUtils.hasText(val) ? val : null;
            }
        } catch (Exception e) {
            log.debug("parse remoteTaskId failed: {}", e.getMessage());
        }
        return null;
    }

    private void markAiTaskProcessing(MemoryCard memoryCard, String remoteTaskId) {
        if (memoryCard == null || !StringUtils.hasText(memoryCard.getTaskId())) {
            return;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("remoteTaskId", remoteTaskId);
        res.put("status", "PROCESSING");
        try {
            String json = objectMapper.writeValueAsString(res);
            aiTaskService.markRunning(memoryCard.getTaskId());
            aiTaskService.updateResult(memoryCard.getTaskId(), json);
        } catch (Exception e) {
            log.debug("mark ai_task processing failed: {}", e.getMessage());
        }
    }

    private void markAiTaskSuccess(MemoryCard memoryCard, String remoteTaskId, String cosUrl) {
        if (memoryCard == null || !StringUtils.hasText(memoryCard.getTaskId())) {
            return;
        }
        Map<String, Object> res = new HashMap<>();
        if (StringUtils.hasText(remoteTaskId)) {
            res.put("remoteTaskId", remoteTaskId);
        }
        if (StringUtils.hasText(cosUrl)) {
            res.put("cosUrl", cosUrl);
        }
        res.put("status", "SUCCESS");
        try {
            String json = objectMapper.writeValueAsString(res);
            aiTaskService.markSuccess(memoryCard.getTaskId(), json);
        } catch (Exception e) {
            log.debug("mark ai_task success failed: {}", e.getMessage());
        }
    }

    private void markAiTaskFailed(MemoryCard memoryCard, String errorMessage) {
        if (memoryCard == null || !StringUtils.hasText(memoryCard.getTaskId())) {
            return;
        }
        aiTaskService.markFailed(memoryCard.getTaskId(), errorMessage);
    }

    /**
     * 转换为 VO 对象
     */
    private MemoryCardVO convertToVO(MemoryCard memoryCard) {
        MemoryCardVO vo = new MemoryCardVO();
        BeanUtils.copyProperties(memoryCard, vo);
        return vo;
    }

    /**
     * 写入历史版本（当前 MemoryCard + 目标图片 URL）
     */
    private void saveHistory(MemoryCard memoryCard, String imageUrlToSave) {
        MemoryCardHistory history = new MemoryCardHistory();
        history.setTripId(memoryCard.getTripId());
        history.setUserId(memoryCard.getUserId());
        history.setTemplateName(memoryCard.getTemplateName());
        history.setTaskId(memoryCard.getTaskId());
        history.setStatus("success");
        history.setErrorMessage(null);
        history.setRetryCount(memoryCard.getRetryCount());
        history.setImageUrl(imageUrlToSave);
        history.setCreateTime(new Date());
        history.setUpdateTime(new Date());
        memoryCardHistoryMapper.insert(history);
    }

    @Transactional
    public MemoryCardGenerateResponse regenerateMemoryCard(Long tripId, User user) {
        Trip trip = tripService.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }

        List<com.lq.travel.model.vo.TripPhotoVO> photoList = tripPhotoService.getTripPhotos(tripId);
        List<String> photoUrls = photoList.stream().map(com.lq.travel.model.vo.TripPhotoVO::getPhotoUrl).collect(Collectors.toList());
        if (photoUrls == null || photoUrls.size() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先为行程关联至少3张照片");
        }

        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0));
        if (memoryCard == null) {
            memoryCard = new MemoryCard();
            memoryCard.setTripId(tripId);
            memoryCard.setUserId(user.getId());
            memoryCard.setTemplateName("default");
        } else if ("success".equals(memoryCard.getStatus()) && memoryCard.getImageUrl() != null) {
            saveHistory(memoryCard, memoryCard.getImageUrl());
        }

        String prompt = buildImagePrompt(trip, photoUrls);
        ImageGenerationService.ImageGenerationRequest imageRequest = new ImageGenerationService.ImageGenerationRequest();
        imageRequest.setPrompt(prompt);
        imageRequest.setReferenceImageUrls(photoUrls);
        imageRequest.setSize("1328x1328");
        imageRequest.setStyle("fresh");
        imageRequest.setQuality("standard");

        String newTaskId = imageGenerationService.generateImageAsync(imageRequest);
        memoryCard.setTaskId(newTaskId);
        memoryCard.setRemoteTaskId(newTaskId);
        memoryCard.setStatus("pending");
        memoryCard.setImageUrl(null);
        memoryCard.setErrorMessage(null);
        memoryCard.setRetryCount(0);
        this.saveOrUpdate(memoryCard);

        MemoryCardGenerateResponse response = new MemoryCardGenerateResponse();
        response.setTaskId(newTaskId);
        response.setStatus("pending");
        return response;
    }

    public List<MemoryCardHistory> listHistoryByTripId(Long tripId, Long userId) {
        Trip trip = tripService.getById(tripId);
        if (trip == null || !trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该行程历史");
        }
        return memoryCardHistoryMapper.selectList(new QueryWrapper<MemoryCardHistory>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0)
                .orderByDesc("create_time"));
    }

    @Transactional
    public boolean setCurrentFromHistory(Long historyId, Long userId) {
        MemoryCardHistory history = memoryCardHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "历史版本不存在");
        }
        Trip trip = tripService.getById(history.getTripId());
        if (trip == null || !trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", history.getTripId())
                .eq("is_delete", 0));
        if (memoryCard == null) {
            memoryCard = new MemoryCard();
            memoryCard.setTripId(history.getTripId());
            memoryCard.setUserId(history.getUserId());
            memoryCard.setTemplateName(history.getTemplateName());
        }
        memoryCard.setStatus("success");
        memoryCard.setImageUrl(history.getImageUrl());
        memoryCard.setErrorMessage(null);
        this.saveOrUpdate(memoryCard);
        return true;
    }

    // === Consumer 辅助能力 ===

    @Override
    public UploadPictureResult uploadToCos(String imageUrl, String pathPrefix) {
        return urlPictureUpload.uploadPicture(imageUrl, pathPrefix);
    }

    @Override
    @Transactional
    public void updateMemoryCardSuccess(Long tripId, String cosUrl, String remoteTaskId) {
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0));
        if (memoryCard == null) {
            log.warn("MemoryCard not found for tripId={}, skip success update", tripId);
            return;
        }
        memoryCard.setStatus("success");
        memoryCard.setImageUrl(cosUrl);
        memoryCard.setErrorMessage(null);
        memoryCard.setRemoteTaskId(remoteTaskId);
        this.updateById(memoryCard);
        markAiTaskSuccess(memoryCard, remoteTaskId, cosUrl);
        saveHistory(memoryCard, cosUrl);
    }

    @Override
    @Transactional
    public void updateMemoryCardFailed(Long tripId, String errorMessage) {
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0));
        if (memoryCard == null) {
            log.warn("MemoryCard not found for tripId={}, skip failed update", tripId);
            return;
        }
        memoryCard.setStatus("failed");
        memoryCard.setErrorMessage(errorMessage);
        this.updateById(memoryCard);
        markAiTaskFailed(memoryCard, errorMessage);
    }

    @Override
    @Transactional
    public void updateMemoryCardProcessing(Long tripId, String remoteTaskId) {
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0));
        if (memoryCard == null) {
            return;
        }
        memoryCard.setStatus("processing");
        memoryCard.setRemoteTaskId(remoteTaskId);
        memoryCard.setErrorMessage(null);
        this.updateById(memoryCard);
        markAiTaskProcessing(memoryCard, remoteTaskId);
    }
}
