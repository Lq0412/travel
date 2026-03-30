package com.lq.travel.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.constant.AiMqConstants;
import com.lq.travel.mapper.KnowledgeExperienceMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.model.entity.KnowledgeExperience;
import com.lq.travel.provider.DashScopeProvider;
import com.lq.travel.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mq", name = "enabled", havingValue = "true")
public class KnowledgeIngestionListener {

    private static final String STATUS_PENDING = "PENDING";
    private static final int ERROR_MSG_MAX_LEN = 512;
    private static final int DASHSCOPE_MIN_TOKENS = 1;
    private static final int DASHSCOPE_MAX_TOKENS = 2000;

    private static final String SOURCE_AUTO = "AUTO";
    private static final String SOURCE_TAVILY = "TAVILY";
    private static final String SOURCE_DASHSCOPE = "DASHSCOPE";

    private static final String EFFECT_FAST = "FAST";
    private static final String EFFECT_BALANCED = "BALANCED";
    private static final String EFFECT_DEEP = "DEEP";

    private final AiTaskService aiTaskService;
    private final KnowledgeExperienceMapper experienceMapper;
    private final DashScopeProvider dashScopeProvider;
    private final TaskMessageProducer taskMessageProducer;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${ai.search.tavily.api-key:${TAVILY_API_KEY:}}")
    private String tavilyApiKey;

    @Value("${ai.search.tavily.endpoint:https://api.tavily.com/search}")
    private String tavilySearchEndpoint;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    @RabbitListener(queues = AiMqConstants.QUEUE_KNOWLEDGE_INGESTION)
    public void handleKnowledgeIngestion(Map<String, Object> payload) {
        String taskId = asString(payload.get("taskId"));
        if (!StringUtils.hasText(taskId)) {
            log.warn("Knowledge Ingestion Task skipped: no taskId in payload");
            return;
        }

        try {
            boolean marked = aiTaskService.markRunning(taskId);
            if (!marked) {
                log.warn("Knowledge Ingestion Task {} skipped because status is not PENDING", taskId);
                return;
            }
            log.info("Knowledge Ingestion Task {} started.", taskId);

            String query = asString(payload.get("query"));
            if (!StringUtils.hasText(query)) {
                throw new IllegalArgumentException("Query cannot be empty");
            }
            query = query.trim();

            String dataSource = normalizeSource(asString(payload.get("dataSource")));
            String effectPreset = normalizeEffect(asString(payload.get("effectPreset")));
            int maxItems = asInt(payload.get("maxItems"), 10, 1, 30);
            boolean mustContainStoreName = asBoolean(payload.get("mustContainStoreName"), true);

            String searchResult = null;
            String sourcePlatform = "dashscope_ai_synthetic";

            if (SOURCE_AUTO.equals(dataSource) || SOURCE_TAVILY.equals(dataSource)) {
                searchResult = fetchTavilySearch(query, maxItems);
                if (StringUtils.hasText(searchResult)) {
                    sourcePlatform = "tavily_search_clean";
                }
            }

            if (SOURCE_TAVILY.equals(dataSource) && !StringUtils.hasText(searchResult)) {
                if (!StringUtils.hasText(tavilyApiKey)) {
                    throw new IllegalStateException("TAVILY 数据源不可用：未检测到 API Key，请配置 ai.search.tavily.api-key 或环境变量 TAVILY_API_KEY");
                }
                throw new IllegalStateException("TAVILY 数据源不可用（可能是 API Key 无效/过期、额度耗尽，或网络不可达）");
            }

            if (SOURCE_DASHSCOPE.equals(dataSource)) {
                searchResult = null;
                sourcePlatform = "dashscope_ai_synthetic";
            }

            PromptConfig promptConfig = buildPromptConfig(query, searchResult, effectPreset, maxItems, mustContainStoreName);
                int safeMaxTokens = clampDashScopeMaxTokens(promptConfig.maxTokens());
                if (safeMaxTokens != promptConfig.maxTokens()) {
                log.warn("Adjust maxTokens for taskId={}, requested={}, adjusted={}",
                    taskId,
                    promptConfig.maxTokens(),
                    safeMaxTokens);
                }

            AIRequest aiRequest = AIRequest.builder()
                    .systemPrompt(promptConfig.systemPrompt())
                    .message(promptConfig.userPrompt())
                    .temperature(promptConfig.temperature())
                    .maxTokens(safeMaxTokens)
                    .build();

            AIResponse aiResponse = dashScopeProvider.chat(aiRequest);
            if (!Boolean.TRUE.equals(aiResponse.getSuccess()) || !StringUtils.hasText(aiResponse.getContent())) {
                throw new RuntimeException("DashScope AI 回答生成失败: " + aiResponse.getErrorMessage());
            }

            String finalContent = aiResponse.getContent();
            String title = "关于 " + query + " 的数据补齐结果（" + effectPreset + "）";

            KnowledgeExperience exp = new KnowledgeExperience();
            exp.setPlatform(sourcePlatform);
            exp.setTitle(title);
            exp.setContent(finalContent);
            exp.setTags(query.replace(" ", ","));
            exp.setSyncStatus(0);
            exp.setCreateTime(LocalDateTime.now());
            exp.setUpdateTime(LocalDateTime.now());

            experienceMapper.insert(exp);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("insertedId", exp.getId());
            result.put("title", title);
            result.put("source", sourcePlatform);
            result.put("configuredSource", dataSource);
            result.put("effectPreset", effectPreset);
            result.put("maxItems", maxItems);
            String resultJson = objectMapper.writeValueAsString(result);
            aiTaskService.markSuccess(taskId, resultJson);

            log.info("Knowledge Ingestion Task {} completed successfully.", taskId);

        } catch (Exception e) {
            log.error("Knowledge Ingestion Task {} failed", taskId, e);
            handleFailure(taskId, payload, e);
        }
    }

    private void handleFailure(String taskId, Map<String, Object> payload, Exception exception) {
        String errorMessage = buildErrorMessage(exception);
        Optional<AiTask> taskOpt = aiTaskService.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            log.error("Knowledge Ingestion Task {} failure ignored: task not found", taskId);
            return;
        }

        if (isPermanentFailure(exception)) {
            log.warn("Knowledge Ingestion Task {} hit permanent error, no retry. error={}", taskId, errorMessage);
            aiTaskService.markFailed(taskId, errorMessage);
            return;
        }

        AiTask task = taskOpt.get();
        int currentRetry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetry = task.getMaxRetry() == null ? 3 : Math.max(1, task.getMaxRetry());

        // 允许最多 maxRetry 次执行（包含首次执行）
        if (currentRetry + 1 < maxRetry) {
            task.setRetryCount(currentRetry + 1);
            task.setStatus(STATUS_PENDING);
            task.setErrorMessage(errorMessage);
            aiTaskService.updateById(task);

            Map<String, Object> retryPayload = new LinkedHashMap<>(payload);
            retryPayload.put("taskId", taskId);
            retryPayload.put("retryAttempt", currentRetry + 1);
            taskMessageProducer.sendKnowledgeIngestionTask(retryPayload);
            log.warn("Knowledge Ingestion Task {} requeued, retryAttempt={}/{}", taskId, currentRetry + 1, maxRetry);
            return;
        }

        aiTaskService.markFailed(taskId, errorMessage);
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getClass().getSimpleName() + ": " + defaultText(exception.getMessage(), "unknown error");
        if (message.length() <= ERROR_MSG_MAX_LEN) {
            return message;
        }
        return message.substring(0, ERROR_MSG_MAX_LEN);
    }

    private String fetchTavilySearch(String query, int maxItems) {
        if (!StringUtils.hasText(tavilyApiKey)) {
            log.warn("Tavily 搜索跳过：未配置 API Key（请设置 ai.search.tavily.api-key 或 TAVILY_API_KEY）");
            return null;
        }

        try {
            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("query", query + " 游记 避坑指南 真实体验");
            bodyMap.put("include_answer", true);
            bodyMap.put("search_depth", "advanced");
            bodyMap.put("max_results", Math.max(3, Math.min(maxItems, 10)));

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(bodyMap), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(tavilySearchEndpoint)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + tavilyApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.body() == null ? "" : response.body().string();
                    log.warn("Tavily 请求失败: code={}, endpoint={}, body={}",
                            response.code(),
                            tavilySearchEndpoint,
                            truncateForLog(errorBody, 300));
                    return null;
                }

                String responseBody = response.body().string();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                String answer = asString(responseMap.get("answer"));
                if (StringUtils.hasText(answer)) {
                    return answer;
                }

                Object resultsObj = responseMap.get("results");
                if (resultsObj instanceof List<?> results) {
                    return results.stream()
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .map(item -> asString(item.get("content")))
                            .filter(StringUtils::hasText)
                            .limit(maxItems)
                            .collect(Collectors.joining("\n\n"));
                }
            }
        } catch (Exception e) {
            log.warn("Tavily 搜索失败，降级为 DashScope 合成：{}", e.getMessage());
        }

        return null;
    }

    private String truncateForLog(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }

    private PromptConfig buildPromptConfig(String query,
                                           String searchResult,
                                           String effectPreset,
                                           int maxItems,
                                           boolean mustContainStoreName) {
        String effectInstruction;
        double temperature;
        int maxTokens;

        switch (effectPreset) {
            case EFFECT_FAST -> {
                effectInstruction = "输出 5 条以内，尽量短句，优先高价值避坑信息。";
                temperature = 0.3;
                maxTokens = 900;
            }
            case EFFECT_DEEP -> {
                effectInstruction = "输出不少于 12 条，覆盖交通、预算、餐饮、住宿、避坑，且给出可执行建议。";
                temperature = 0.45;
                maxTokens = 1800;
            }
            default -> {
                effectInstruction = "输出 8-10 条，平衡信息密度与可读性，包含避坑和体验建议。";
                temperature = 0.5;
                maxTokens = 1500;
            }
        }

        if (maxItems > 0) {
            effectInstruction += " 最终条目数不要超过 " + maxItems + " 条。";
        }
        if (mustContainStoreName) {
            effectInstruction += " 优先包含真实店名、街道名或地标。";
        }

        if (StringUtils.hasText(searchResult)) {
            String systemPrompt = "你是旅游数据清洗专家。请仅输出可执行建议，不要输出空泛表述。";
            String userPrompt = "请基于以下抓取数据整理目的地经验。" + effectInstruction + "\n\n原始资料:\n" + searchResult;
            return new PromptConfig(systemPrompt, userPrompt, temperature, maxTokens);
        }

        String systemPrompt = "你是资深旅行顾问，请输出结构化且可落地的旅行经验。";
        String userPrompt = "请围绕【" + query + "】生成数据补齐内容。" + effectInstruction;
        return new PromptConfig(systemPrompt, userPrompt, temperature, maxTokens);
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return SOURCE_AUTO;
        }
        String value = source.trim().toUpperCase();
        if (SOURCE_AUTO.equals(value) || SOURCE_TAVILY.equals(value) || SOURCE_DASHSCOPE.equals(value)) {
            return value;
        }
        return SOURCE_AUTO;
    }

    private String normalizeEffect(String effect) {
        if (!StringUtils.hasText(effect)) {
            return EFFECT_BALANCED;
        }
        String value = effect.trim().toUpperCase();
        if (EFFECT_FAST.equals(value) || EFFECT_BALANCED.equals(value) || EFFECT_DEEP.equals(value)) {
            return value;
        }
        return EFFECT_BALANCED;
    }

    private int asInt(Object value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        int parsed;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else {
            try {
                parsed = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return Math.max(min, Math.min(max, parsed));
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int clampDashScopeMaxTokens(int requestedMaxTokens) {
        return Math.max(DASHSCOPE_MIN_TOKENS, Math.min(requestedMaxTokens, DASHSCOPE_MAX_TOKENS));
    }

    private boolean isPermanentFailure(Exception exception) {
        StringBuilder sb = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                sb.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        String allMessages = sb.toString().toLowerCase();
        return allMessages.contains("invalidparameter")
                || allMessages.contains("range of max_tokens")
            || allMessages.contains("max_tokens should be")
            || allMessages.contains("unauthorized")
            || allMessages.contains("statuscode\":401")
            || allMessages.contains("status=401")
            || allMessages.contains("tavily 数据源不可用");
    }

    private record PromptConfig(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
    }
}
