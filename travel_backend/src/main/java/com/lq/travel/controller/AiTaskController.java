package com.lq.travel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.ai.KnowledgeIngestionTaskCreateRequest;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.model.vo.AiTaskStatusVO;
import com.lq.travel.mq.TaskMessageProducer;
import com.lq.travel.service.AiTaskService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 任务查询接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/tasks")
public class AiTaskController {

    private static final String TASK_TYPE_KNOWLEDGE_INGESTION = "KNOWLEDGE_INGESTION";
    private static final String SOURCE_AUTO = "AUTO";
    private static final String EFFECT_BALANCED = "BALANCED";

    @Resource
    private AiTaskService aiTaskService;

    @Resource
    private TaskMessageProducer taskMessageProducer;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/knowledge/ingest")
    public Object submitIngestionTask(
            @RequestBody(required = false) @Valid KnowledgeIngestionTaskCreateRequest request,
            @RequestParam(required = false) String query
    ) {
        String finalQuery = resolveQuery(request, query);
        if (!StringUtils.hasText(finalQuery)) {
            return ResponseUtils.error(400, "query 不能为空");
        }

        String dataSource = normalizeDataSource(request != null ? request.getDataSource() : null);
        String effectPreset = normalizeEffectPreset(request != null ? request.getEffectPreset() : null);
        int maxItems = clampInt(request != null ? request.getMaxItems() : null, 10, 1, 30);
        int maxRetry = clampInt(request != null ? request.getMaxRetry() : null, 3, 1, 10);
        boolean mustContainStoreName = request == null || request.getMustContainStoreName() == null || request.getMustContainStoreName();

        String taskId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("query", finalQuery);
        payload.put("dataSource", dataSource);
        payload.put("effectPreset", effectPreset);
        payload.put("maxItems", maxItems);
        payload.put("mustContainStoreName", mustContainStoreName);
        payload.put("maxRetry", maxRetry);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("创建数据补齐任务失败：payload 序列化失败", e);
            return ResponseUtils.error(500, "任务创建失败：payload 序列化失败");
        }

        String bizKey = buildBizKey(finalQuery, dataSource, effectPreset, maxItems, mustContainStoreName);

        AiTask persistedTask = aiTaskService.createTask(
                taskId,
                bizKey,
                TASK_TYPE_KNOWLEDGE_INGESTION,
                payloadJson,
                0L,
                null,
                maxRetry
        );

        boolean deduplicated = !taskId.equals(persistedTask.getTaskId());
        String effectiveTaskId = persistedTask.getTaskId();

        if (deduplicated) {
            Map<String, Object> existed = new LinkedHashMap<>();
            existed.put("taskId", effectiveTaskId);
            existed.put("status", persistedTask.getStatus());
            existed.put("deduplicated", true);
            existed.put("dataSource", dataSource);
            existed.put("effectPreset", effectPreset);
            existed.put("maxItems", maxItems);
            existed.put("maxRetry", persistedTask.getMaxRetry());
            existed.put("mustContainStoreName", mustContainStoreName);
            return ResponseUtils.success(existed);
        }

        payload.put("taskId", effectiveTaskId);
        taskMessageProducer.sendKnowledgeIngestionTask(payload);

        return ResponseUtils.success(Map.of(
                "taskId", effectiveTaskId,
                "status", "SUBMITTED",
                "deduplicated", false,
                "dataSource", dataSource,
                "effectPreset", effectPreset,
                "maxItems", maxItems,
                "maxRetry", maxRetry,
                "mustContainStoreName", mustContainStoreName
        ));
    }

    @GetMapping("/{taskId}")
    public Object getTaskStatus(@PathVariable String taskId) {
        Optional<AiTask> taskOpt = aiTaskService.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseUtils.error(404, "Task not found");
        }
        AiTask task = taskOpt.get();
        AiTaskStatusVO vo = new AiTaskStatusVO();
        vo.setTaskId(task.getTaskId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setPayload(task.getPayload());
        vo.setResult(task.getResult());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setUpdateTime(task.getUpdateTime());
        return ResponseUtils.success(vo);
    }

    private String resolveQuery(KnowledgeIngestionTaskCreateRequest request, String query) {
        if (request != null && StringUtils.hasText(request.getQuery())) {
            return request.getQuery().trim();
        }
        if (StringUtils.hasText(query)) {
            return query.trim();
        }
        return null;
    }

    private String normalizeDataSource(String source) {
        if (!StringUtils.hasText(source)) {
            return SOURCE_AUTO;
        }
        String value = source.trim().toUpperCase();
        if ("TAVILY".equals(value) || "DASHSCOPE".equals(value) || SOURCE_AUTO.equals(value)) {
            return value;
        }
        return SOURCE_AUTO;
    }

    private String normalizeEffectPreset(String effectPreset) {
        if (!StringUtils.hasText(effectPreset)) {
            return EFFECT_BALANCED;
        }
        String value = effectPreset.trim().toUpperCase();
        if ("FAST".equals(value) || EFFECT_BALANCED.equals(value) || "DEEP".equals(value)) {
            return value;
        }
        return EFFECT_BALANCED;
    }

    private int clampInt(Integer value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String buildBizKey(String query,
                               String dataSource,
                               String effectPreset,
                               int maxItems,
                               boolean mustContainStoreName) {
        String raw = String.join("|",
                defaultText(query),
                defaultText(dataSource),
                defaultText(effectPreset),
                String.valueOf(maxItems),
                String.valueOf(mustContainStoreName));
        return "INGEST_" + UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
