package com.lq.travel.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.AI.core.service.ImageGenerationService;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.service.AiTaskService;
import com.lq.travel.service.MemoryCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AiTask 与远端任务的补偿调度（RUNNING -> SUCCESS/FAILED）
 * 仅在开启 MQ 时生效
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mq", name = "enabled", havingValue = "true")
public class AiTaskReconcileScheduler {

    private final AiTaskService aiTaskService;
    private final MemoryCardService memoryCardService;
    private final ImageGenerationService imageGenerationService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${ai.mq.reconcile-interval-ms:60000}")
    public void reconcileRunningTasks() {
        List<AiTask> runningTasks = aiTaskService.list(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStatus, "RUNNING"));
        if (runningTasks.isEmpty()) {
            return;
        }
        for (AiTask task : runningTasks) {
            String remoteTaskId = extractRemoteTaskId(task.getResult());
            if (remoteTaskId == null) {
                log.warn("Skip task {} (no remoteTaskId in result)", task.getTaskId());
                continue;
            }
            try {
                ImageGenerationService.ImageGenerationResult result = imageGenerationService.getTaskStatus(remoteTaskId);
                if ("success".equalsIgnoreCase(result.getStatus()) && result.getImageUrl() != null) {
                    String cosPath = "memory-card/" + (task.getTripId() != null ? task.getTripId() : "unknown");
                    UploadPictureResult uploadResult = memoryCardService.uploadToCos(result.getImageUrl(), cosPath);
                    if (task.getTripId() != null) {
                        memoryCardService.updateMemoryCardSuccess(task.getTripId(), uploadResult.getUrl(), remoteTaskId);
                    }
                    aiTaskService.markSuccess(task.getTaskId(), buildResultJson(remoteTaskId, uploadResult.getUrl()));
                } else if ("failed".equalsIgnoreCase(result.getStatus())) {
                    String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "remote task failed";
                    if (task.getTripId() != null) {
                        memoryCardService.updateMemoryCardFailed(task.getTripId(), errorMsg);
                    }
                    aiTaskService.markFailed(task.getTaskId(), errorMsg);
                } else {
                    // 仍在处理，更新结果中的状态，等待下次调度
                    aiTaskService.updateResult(task.getTaskId(),
                            "{\"remoteTaskId\":\"" + remoteTaskId + "\",\"status\":\"PROCESSING\"}");
                }
            } catch (Exception e) {
                log.error("Reconcile task {} failed", task.getTaskId(), e);
            }
        }
    }

    private String extractRemoteTaskId(String result) {
        if (result == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(result);
            JsonNode remote = node.get("remoteTaskId");
            return remote != null ? remote.asText(null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildResultJson(String remoteTaskId, String cosUrl) {
        return "{\"remoteTaskId\":\"" + remoteTaskId + "\",\"cosUrl\":\"" + cosUrl + "\"}";
    }
}
