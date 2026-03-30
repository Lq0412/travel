package com.lq.travel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope 向量化服务
 */
@Slf4j
@Service
public class DashScopeEmbeddingService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String TEXT_EMBEDDING_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String MULTIMODAL_EMBEDDING_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    private static final int MAX_EMBEDDING_TEXT_LENGTH = 2000;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v1}")
    private String embeddingModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * 获取输入文本的 embedding 向量。
     */
    public List<Float> embed(String text) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(text)) {
            return List.of();
        }

        String normalizedText = normalizeText(text);
        String modelToUse = StringUtils.hasText(embeddingModel) ? embeddingModel.trim() : "text-embedding-v1";
        boolean useMultimodal = isMultimodalModel(modelToUse);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelToUse);
        if (useMultimodal) {
            payload.put("input", Map.of("contents", List.of(Map.of("text", normalizedText))));
        } else {
            payload.put("input", Map.of("texts", List.of(normalizedText)));
        }

        try {
            String requestJson = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(useMultimodal ? MULTIMODAL_EMBEDDING_URL : TEXT_EMBEDDING_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("DashScope embedding 调用失败: status={}", response.code());
                    return List.of();
                }
                return parseEmbedding(response.body().string());
            }
        } catch (Exception e) {
            log.warn("DashScope embedding 调用异常: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Float> parseEmbedding(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode embeddingNode = root.path("output").path("embeddings");
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            return List.of();
        }

        JsonNode vectorNode = embeddingNode.get(0).path("embedding");
        if (!vectorNode.isArray() || vectorNode.isEmpty()) {
            return List.of();
        }

        List<Float> vector = new ArrayList<>(vectorNode.size());
        for (JsonNode item : vectorNode) {
            vector.add((float) item.asDouble());
        }
        return vector;
    }

    private String normalizeText(String text) {
        String collapsed = text.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_EMBEDDING_TEXT_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_EMBEDDING_TEXT_LENGTH);
    }

    private boolean isMultimodalModel(String model) {
        String normalized = model.toLowerCase();
        return normalized.contains("multimodal")
                || normalized.contains("-vl-")
                || normalized.contains("vision");
    }
}
