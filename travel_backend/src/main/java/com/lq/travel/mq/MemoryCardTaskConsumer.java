package com.lq.travel.mq;

import com.lq.travel.AI.core.service.ImageGenerationService;
import com.lq.travel.constant.AiMqConstants;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.service.AiTaskService;
import com.lq.travel.service.MemoryCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 回忆图任务消费者（本地 taskId 为准，远端 taskId 存储在 result）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mq", name = "enabled", havingValue = "true")
public class MemoryCardTaskConsumer {

    private final AiTaskService aiTaskService;
    private final MemoryCardService memoryCardService;
    private final ImageGenerationService imageGenerationService;

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PROCESSING = "PROCESSING";

    // 简单的轮询配置（可根据需要调整或改为延迟队列重试）
    private static final int POLL_MAX_ATTEMPTS = 3;
    private static final long POLL_INTERVAL_MS = 2000L;

    @RabbitListener(queues = AiMqConstants.QUEUE_MEMORY_CARD, autoStartup = "true")
    public void handleMemoryCard(@Payload Map<String, Object> message,
                                 @Header(name = "x-retry-count", required = false) Integer retryCount) {
        String taskId = getString(message, "taskId");
        if (taskId == null) {
            log.warn("Memory-card message missing taskId, skip. msg={}", message);
            return;
        }
        Optional<AiTask> taskOpt = aiTaskService.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            log.warn("AiTask not found for taskId={}, skip.", taskId);
            return;
        }
        AiTask task = taskOpt.get();

        // 幂等：已完成的不处理
        if (STATUS_SUCCESS.equalsIgnoreCase(task.getStatus()) || STATUS_FAILED.equalsIgnoreCase(task.getStatus())) {
            log.info("Task {} already finished with status {}, skip", taskId, task.getStatus());
            return;
        }

        // 标记 RUNNING
        aiTaskService.markRunning(taskId);

        try {
            // 解析 payload
            MemoryCardGenerateRequest req = mapToRequest(message);
            ImageGenerationService.ImageGenerationRequest imageRequest = new ImageGenerationService.ImageGenerationRequest();
            imageRequest.setPrompt(getString(message, "prompt"));
            imageRequest.setReferenceImageUrls(req.getPhotoUrls());
            imageRequest.setSize(req.getSize() != null ? req.getSize() : "1328x1328");
            imageRequest.setStyle(req.getStyle() != null ? req.getStyle() : "fresh");
            imageRequest.setQuality(req.getQuality() != null ? req.getQuality() : "standard");

            // 调用 DashScope
            String remoteTaskId = imageGenerationService.generateImageAsync(imageRequest);

            // 标记 processing 并记录远端 taskId
            memoryCardService.updateMemoryCardProcessing(req.getTripId(), remoteTaskId);
            aiTaskService.updateResult(taskId, "{\"remoteTaskId\":\"" + remoteTaskId + "\",\"status\":\"PROCESSING\"}");

            // 轮询远端状态（有限次数）；若仍 processing，由定时任务/后续拉起继续更新
            ImageGenerationService.ImageGenerationResult result = pollStatus(remoteTaskId);
            if (STATUS_SUCCESS.equalsIgnoreCase(result.getStatus()) && result.getImageUrl() != null) {
                UploadPictureResult uploadResult = memoryCardService.uploadToCos(result.getImageUrl(),
                        "memory-card/" + req.getTripId());
                memoryCardService.updateMemoryCardSuccess(req.getTripId(), uploadResult.getUrl(), remoteTaskId);
                aiTaskService.markSuccess(taskId, "{\"remoteTaskId\":\"" + remoteTaskId + "\",\"cosUrl\":\"" + uploadResult.getUrl() + "\"}");
            } else if (STATUS_FAILED.equalsIgnoreCase(result.getStatus())) {
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "remote task failed";
                memoryCardService.updateMemoryCardFailed(req.getTripId(), errorMsg);
                aiTaskService.markFailed(taskId, errorMsg);
            } else {
                // 留待后续定时任务或重试消费继续更新
                aiTaskService.updateResult(taskId, "{\"remoteTaskId\":\"" + remoteTaskId + "\",\"status\":\"PROCESSING\"}");
                log.info("Remote task processing, taskId={}, remoteTaskId={}, status={}", taskId, remoteTaskId, result.getStatus());
            }

        } catch (Exception e) {
            log.error("Memory-card task {} failed", taskId, e);
            aiTaskService.markFailed(taskId, e.getMessage());
            memoryCardService.updateMemoryCardFailed(taskOpt.get().getTripId(), e.getMessage());
        }
    }

    private ImageGenerationService.ImageGenerationResult pollStatus(String remoteTaskId) throws InterruptedException {
        ImageGenerationService.ImageGenerationResult result = imageGenerationService.getTaskStatus(remoteTaskId);
        int attempt = 1;
        while (attempt < POLL_MAX_ATTEMPTS && STATUS_PROCESSING.equalsIgnoreCase(result.getStatus())) {
            TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            result = imageGenerationService.getTaskStatus(remoteTaskId);
            attempt++;
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private MemoryCardGenerateRequest mapToRequest(Map<String, Object> msg) {
        MemoryCardGenerateRequest req = new MemoryCardGenerateRequest();
        Object tripId = msg.get("tripId");
        req.setTripId(tripId == null ? null : Long.valueOf(String.valueOf(tripId)));
        Object templateName = msg.get("templateName");
        req.setTemplateName(templateName != null ? String.valueOf(templateName) : null);
        Object size = msg.get("size");
        req.setSize(size != null ? String.valueOf(size) : null);
        Object style = msg.get("style");
        req.setStyle(style != null ? String.valueOf(style) : null);
        Object quality = msg.get("quality");
        req.setQuality(quality != null ? String.valueOf(quality) : null);
        Object photos = msg.get("photoUrls");
        if (photos instanceof List<?>) {
            req.setPhotoUrls(((List<?>) photos).stream().map(String::valueOf).toList());
        } else {
            req.setPhotoUrls(Collections.emptyList());
        }
        return req;
    }
}