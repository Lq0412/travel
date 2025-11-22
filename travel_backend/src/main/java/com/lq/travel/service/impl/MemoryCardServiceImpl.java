package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.AI.core.service.ImageGenerationService;
import com.lq.travel.manager.upload.UrlPictureUpload;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.MemoryCardMapper;
import com.lq.travel.mapper.MemoryCardHistoryMapper;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.dto.trip.MemoryCardGenerateResponse;
import com.lq.travel.model.entity.MemoryCard;
import com.lq.travel.model.entity.MemoryCardHistory;
import com.lq.travel.model.entity.Trip;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;
import com.lq.travel.service.MemoryCardService;
import com.lq.travel.service.TripService;
import com.lq.travel.service.TripPhotoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
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
    private UrlPictureUpload urlPictureUpload;
    
    @Resource
    private MemoryCardHistoryMapper memoryCardHistoryMapper;

    @Resource
    private TripPhotoService tripPhotoService;
    
    @Override
    @Transactional
    public MemoryCardGenerateResponse generateMemoryCard(MemoryCardGenerateRequest request, User user) {
        // 参数校验
        if (request.getTripId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "行程ID不能为空");
        }
        
        if (request.getPhotoUrls() == null || request.getPhotoUrls().size() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要3张照片");
        }
        
        if (request.getPhotoUrls().size() > 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多只能使用6张照片");
        }
        
        // 验证行程
        Trip trip = tripService.getById(request.getTripId());
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }
        
        // 检查是否已有回忆图
        QueryWrapper<MemoryCard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", request.getTripId());
        queryWrapper.eq("is_delete", 0);
        MemoryCard existingCard = this.getOne(queryWrapper);
        
        if (existingCard != null && "success".equals(existingCard.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该行程已生成回忆图");
        }
        
        // 创建或更新回忆图记录
        MemoryCard memoryCard;
        if (existingCard != null) {
            memoryCard = existingCard;
        } else {
            memoryCard = new MemoryCard();
            memoryCard.setTripId(request.getTripId());
            memoryCard.setUserId(user.getId());
            memoryCard.setTemplateName(request.getTemplateName() != null ? request.getTemplateName() : "default");
        }
        
        memoryCard.setStatus("pending");
        memoryCard.setRetryCount(0);
        memoryCard.setErrorMessage(null);
        
        // 构建图像生成提示词
        String prompt = buildImagePrompt(trip, request.getPhotoUrls());
        
        // 调用图像生成服务
        ImageGenerationService.ImageGenerationRequest imageRequest = 
            new ImageGenerationService.ImageGenerationRequest();
        imageRequest.setPrompt(prompt);
        imageRequest.setReferenceImageUrls(request.getPhotoUrls());
        // DashScope 允许的尺寸之一：1328x1328（将在实现中被规范化为 1328*1328）
        imageRequest.setSize("1328x1328");
        imageRequest.setStyle("fresh"); // MVP默认清新风格
        imageRequest.setQuality("standard");
        
        try {
            String taskId = imageGenerationService.generateImageAsync(imageRequest);
            memoryCard.setTaskId(taskId);
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
    
    /**
     * 构建图像生成提示词
     */
    private String buildImagePrompt(Trip trip, List<String> photoUrls) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("生成一张旅行回忆卡片，包含以下元素：\n");
        prompt.append("目的地：").append(trip.getDestination()).append("\n");
        prompt.append("天数：").append(trip.getDays()).append("天\n");
        if (trip.getTheme() != null) {
            prompt.append("主题：").append(trip.getTheme()).append("\n");
        }
        prompt.append("风格：清新、温馨、充满回忆感\n");
        prompt.append("参考照片：").append(photoUrls.size()).append("张旅行照片\n");
        prompt.append("要求：将照片元素融合到卡片设计中，整体风格统一，适合作为旅行纪念");
        
        return prompt.toString();
    }
    
    @Override
    public MemoryCardVO getMemoryCardStatus(String taskId, Long userId) {
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
            .eq("task_id", taskId)
            .eq("is_delete", 0));
        
        if (memoryCard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回忆图不存在");
        }
        
        if (!memoryCard.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该回忆图");
        }
        
        // 查询任务状态
        ImageGenerationService.ImageGenerationResult taskResult = 
            imageGenerationService.getTaskStatus(taskId);
        
        // 更新回忆图状态
        if (!memoryCard.getStatus().equals(taskResult.getStatus())) {
            memoryCard.setStatus(taskResult.getStatus());
            if ("success".equals(taskResult.getStatus()) && taskResult.getImageUrl() != null) {
                // 成功：将供应商图片上传至COS，使用自有URL
                try {
                    UploadPictureResult uploadResult = urlPictureUpload.uploadPicture(taskResult.getImageUrl(), "memory-card/" + memoryCard.getTripId());
                    String cosUrl = uploadResult.getUrl();
                    saveHistory(memoryCard, cosUrl);
                    memoryCard.setImageUrl(cosUrl);
                } catch (Exception ex) {
                    // 若持久化失败，先回退使用供应商URL，避免前端无图
                    String fallbackUrl = taskResult.getImageUrl();
                    saveHistory(memoryCard, fallbackUrl);
                    memoryCard.setImageUrl(fallbackUrl);
                }
            }
            if ("failed".equals(taskResult.getStatus())) {
                if (taskResult.getErrorMessage() != null) {
                    memoryCard.setErrorMessage(taskResult.getErrorMessage());
                }
            } else {
                // 非失败态统一清空历史错误文案（pending/processing/success）
                memoryCard.setErrorMessage(null);
            }
            this.updateById(memoryCard);
        }
        
        return convertToVO(memoryCard);
    }
    
    /**
     * 重试生成
     */
    private void retryGeneration(MemoryCard memoryCard) {
        log.info("自动重试生成回忆图，任务ID: {}", memoryCard.getTaskId());
        memoryCard.setRetryCount(memoryCard.getRetryCount() + 1);
        memoryCard.setStatus("pending");
        memoryCard.setErrorMessage(null);
        // 这里可以重新调用生成服务
        // 为了简化，暂时只更新状态
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
    @Scheduled(fixedDelay = 30000) // 每30秒执行一次
    public void updateMemoryCardStatus() {
        try {
            // 查询所有pending和processing状态的回忆图
            QueryWrapper<MemoryCard> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("status", "pending", "processing");
            queryWrapper.eq("is_delete", 0);
            
            List<MemoryCard> memoryCards = this.list(queryWrapper);
            
            for (MemoryCard memoryCard : memoryCards) {
                try {
                    if (memoryCard.getTaskId() != null) {
                        ImageGenerationService.ImageGenerationResult taskResult = 
                            imageGenerationService.getTaskStatus(memoryCard.getTaskId());
                        
                        if (!memoryCard.getStatus().equals(taskResult.getStatus())) {
                            memoryCard.setStatus(taskResult.getStatus());
                            if ("success".equals(taskResult.getStatus()) && taskResult.getImageUrl() != null) {
                                try {
                                    UploadPictureResult uploadResult = urlPictureUpload.uploadPicture(taskResult.getImageUrl(), "memory-card/" + memoryCard.getTripId());
                                    String cosUrl = uploadResult.getUrl();
                                    saveHistory(memoryCard, cosUrl);
                                    memoryCard.setImageUrl(cosUrl);
                                } catch (Exception ex) {
                                    String fallbackUrl = taskResult.getImageUrl();
                                    saveHistory(memoryCard, fallbackUrl);
                                    memoryCard.setImageUrl(fallbackUrl);
                                }
                            }
                            if ("failed".equals(taskResult.getStatus())) {
                                if (taskResult.getErrorMessage() != null) {
                                    memoryCard.setErrorMessage(taskResult.getErrorMessage());
                                }
                            } else {
                                // 非失败态统一清空历史错误文案（pending/processing/success）
                                memoryCard.setErrorMessage(null);
                            }
                            this.updateById(memoryCard);
                        }
                    }
                } catch (Exception e) {
                    log.error("更新回忆图状态失败，ID: " + memoryCard.getId(), e);
                }
            }
        } catch (BadSqlGrammarException e) {
            // 表不存在时，静默处理，避免频繁报错
            Throwable rootCause = e.getRootCause();
            if (rootCause != null && rootCause.getMessage() != null && 
                rootCause.getMessage().contains("doesn't exist")) {
                log.debug("回忆图表尚未创建，跳过定时任务执行");
            } else {
                log.error("定时任务执行失败：数据库查询异常", e);
            }
        } catch (DataAccessException e) {
            // 数据库访问异常，可能是表不存在
            Throwable rootCause = e.getRootCause();
            if (rootCause != null && rootCause.getMessage() != null && 
                rootCause.getMessage().contains("doesn't exist")) {
                log.debug("回忆图表尚未创建，跳过定时任务执行");
            } else {
                log.error("定时任务执行失败：数据库访问异常", e);
            }
        } catch (Exception e) {
            // 其他异常正常记录日志
            log.error("定时任务执行失败", e);
        }
    }
    
    /**
     * 转换为VO对象
     */
    private MemoryCardVO convertToVO(MemoryCard memoryCard) {
        MemoryCardVO vo = new MemoryCardVO();
        BeanUtils.copyProperties(memoryCard, vo);
        return vo;
    }

    /**
     * 写入历史版本（基于当前 MemoryCard 和目标图片URL）
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
        // 校验行程与权限
        Trip trip = tripService.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }

        // 读取已关联图片（至少3张）
        List<com.lq.travel.model.vo.TripPhotoVO> photoList = tripPhotoService.getTripPhotos(tripId);
        List<String> photoUrls = photoList.stream().map(com.lq.travel.model.vo.TripPhotoVO::getPhotoUrl).collect(Collectors.toList());
        if (photoUrls == null || photoUrls.size() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先为行程关联至少3张照片");
        }

        // 准备记录
        MemoryCard memoryCard = this.getOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0));
        if (memoryCard == null) {
            memoryCard = new MemoryCard();
            memoryCard.setTripId(tripId);
            memoryCard.setUserId(user.getId());
            memoryCard.setTemplateName("default");
        } else if ("success".equals(memoryCard.getStatus()) && memoryCard.getImageUrl() != null) {
            // 存档历史
            saveHistory(memoryCard, memoryCard.getImageUrl());
        }

        // 构建请求并发起新任务
        String prompt = buildImagePrompt(trip, photoUrls);
        ImageGenerationService.ImageGenerationRequest imageRequest = new ImageGenerationService.ImageGenerationRequest();
        imageRequest.setPrompt(prompt);
        imageRequest.setReferenceImageUrls(photoUrls);
        imageRequest.setSize("1328x1328");
        imageRequest.setStyle("fresh");
        imageRequest.setQuality("standard");

        String newTaskId = imageGenerationService.generateImageAsync(imageRequest);
        memoryCard.setTaskId(newTaskId);
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

    public java.util.List<MemoryCardHistory> listHistoryByTripId(Long tripId, Long userId) {
        // 校验行程归属
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
}

