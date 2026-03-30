package com.lq.travel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.config.RagProperties;
import com.lq.travel.model.dto.ai.RagChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Milvus 检索客户端（REST）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusRagClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String SEARCH_PATH = "/v2/vectordb/entities/search";
    private static final String QUERY_PATH = "/v2/vectordb/entities/query";
    private static final String UPSERT_PATH = "/v2/vectordb/entities/upsert";
    private static final String LIST_COLLECTION_PATH = "/v2/vectordb/collections/list";
    private static final String GET_COLLECTION_STATS_PATH = "/v2/vectordb/collections/get_stats";
    private static final String CREATE_COLLECTION_PATH = "/v2/vectordb/collections/create";
    private static final String DROP_COLLECTION_PATH = "/v2/vectordb/collections/drop";
    private static final String LOAD_COLLECTION_PATH = "/v2/vectordb/collections/load";
    private static final String CREATE_INDEX_PATH = "/v2/vectordb/indexes/create";
    private static final int REQUEST_MAX_RETRIES = 3;
    private static final long REQUEST_RETRY_BACKOFF_MS = 400L;

    private final RagProperties ragProperties;
    private final DashScopeEmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 基于用户查询做向量检索。
     */
    public List<RagChunk> search(String query, int topK) {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        if (!milvus.isEnabled() || !StringUtils.hasText(query)) {
            return List.of();
        }

        List<Float> vector = embeddingService.embed(query);
        if (vector.isEmpty()) {
            return List.of();
        }

        try {
            String endpoint = normalizeEndpoint(milvus.getEndpoint()) + SEARCH_PATH;
            String payload = buildSearchPayload(milvus, vector, topK);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(endpoint)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE));

            if (StringUtils.hasText(milvus.getToken())) {
                requestBuilder.addHeader("Authorization", "Bearer " + milvus.getToken().trim());
            }

            try (Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Milvus 检索失败: status={}", response.code());
                    return List.of();
                }
                return parseSearchResult(response.body().string(), topK);
            }
        } catch (Exception e) {
            log.warn("Milvus 检索异常: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 初始化集合，必要时创建并加载。
     */
    public boolean ensureCollectionReady(int dimension, boolean recreateIfExists) {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        if (!milvus.isEnabled()) {
            return false;
        }
        if (dimension <= 0) {
            log.warn("Milvus 初始化失败: 向量维度非法 dimension={}", dimension);
            return false;
        }

        try {
            boolean exists = collectionExists(milvus);
            if (exists && recreateIfExists) {
                boolean dropped = invokeMilvus(
                        DROP_COLLECTION_PATH,
                        Map.of(
                                "dbName", milvus.getDatabase(),
                                "collectionName", milvus.getCollection()
                        ),
                        true
                );
                if (!dropped) {
                    return false;
                }
                exists = false;
            }

            if (!exists) {
                if (!createCollection(milvus, dimension)) {
                    return false;
                }
                createIndexIfPossible(milvus);
            }

            return loadCollection(milvus);
        } catch (Exception e) {
            log.warn("Milvus 集合初始化异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 批量 upsert 向量文档。
     */
    public boolean upsert(List<Map<String, Object>> dataRows) {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        if (!milvus.isEnabled() || dataRows == null || dataRows.isEmpty()) {
            return false;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dbName", milvus.getDatabase());
        payload.put("collectionName", milvus.getCollection());
        payload.put("data", dataRows);

        return invokeMilvus(UPSERT_PATH, payload, true);
    }

    /**
     * 显式加载集合到内存，便于后续检索立即可用。
     */
    public boolean loadCollection() {
        return loadCollection(ragProperties.getMilvus());
    }

    /**
     * 通过 entities/query 统计当前集合可查询到的实体数量。
     * 说明：部分 Milvus 版本不支持 entities/count，使用 query 作为兼容方案。
     */
    public Map<String, Object> queryEntityCount(int limit) {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        Map<String, Object> result = new LinkedHashMap<>();

        int safeLimit = Math.max(1, Math.min(limit, 5000));
        String primaryField = StringUtils.hasText(milvus.getPrimaryField())
                ? milvus.getPrimaryField().trim()
                : "id";

        result.put("status", "skipped");
        result.put("collection", milvus.getCollection());
        result.put("primaryField", primaryField);
        result.put("limit", safeLimit);

        if (!milvus.isEnabled()) {
            result.put("reason", "milvus_disabled");
            return result;
        }

        JsonNode queryRoot = invokeMilvusForResult(
                QUERY_PATH,
                Map.of(
                        "dbName", milvus.getDatabase(),
                        "collectionName", milvus.getCollection(),
                        "filter", primaryField + " >= 0",
                        "limit", safeLimit,
                        "outputFields", List.of(primaryField)
                ),
                true
        );
        if (queryRoot == null) {
            result.put("status", "failed");
            result.put("reason", "query_failed");
            result.put("countBy", "entities/query");
            return result;
        }

        List<JsonNode> rows = new ArrayList<>();
        collectQueryRows(queryRoot.path("data"), primaryField, rows);

        List<Long> sampleIds = new ArrayList<>();
        for (JsonNode row : rows) {
            JsonNode idNode = row.path(primaryField);
            if (idNode.isMissingNode() || idNode.isNull()) {
                idNode = row.path("id");
            }
            if (idNode.isIntegralNumber()) {
                sampleIds.add(idNode.asLong());
            } else if (idNode.isTextual()) {
                try {
                    sampleIds.add(Long.parseLong(idNode.asText().trim()));
                } catch (NumberFormatException ignored) {
                    // ignore non-numeric id
                }
            }
            if (sampleIds.size() >= 5) {
                break;
            }
        }

        result.put("status", "success");
        result.put("countBy", "entities/query");
        result.put("queryCount", rows.size());
        result.put("truncated", rows.size() >= safeLimit);
        if (!sampleIds.isEmpty()) {
            result.put("sampleIds", sampleIds);
        }

        JsonNode statsRoot = invokeMilvusForResult(
                GET_COLLECTION_STATS_PATH,
                Map.of(
                        "dbName", milvus.getDatabase(),
                        "collectionName", milvus.getCollection()
                ),
                false
        );
        if (statsRoot != null) {
            JsonNode rowCountNode = statsRoot.path("data").path("rowCount");
            if (rowCountNode.canConvertToLong()) {
                result.put("statsRowCount", rowCountNode.asLong());
            }
        }

        return result;
    }

    private String buildSearchPayload(RagProperties.MilvusProperties milvus, List<Float> vector, int topK)
            throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dbName", milvus.getDatabase());
        payload.put("collectionName", milvus.getCollection());
        payload.put("data", List.of(vector));
        payload.put("annsField", milvus.getVectorField());
        payload.put("limit", topK);
        payload.put("outputFields", milvus.getOutputFields());

        Map<String, Object> searchParams = new LinkedHashMap<>();
        searchParams.put("metric_type", "COSINE");
        searchParams.put("params", Map.of("nprobe", milvus.getNprobe()));
        payload.put("searchParams", searchParams);
        payload.put("consistencyLevel", "Bounded");

        return objectMapper.writeValueAsString(payload);
    }

    private boolean collectionExists(RagProperties.MilvusProperties milvus) {
        JsonNode root = invokeMilvusForResult(
                LIST_COLLECTION_PATH,
                Map.of("dbName", milvus.getDatabase()),
                true
        );
        if (root == null) {
            return false;
        }

        JsonNode dataNode = root.path("data");
        Set<String> names = new HashSet<>();
        collectCollectionNames(dataNode, names);
        return names.contains(milvus.getCollection());
    }

    private void collectCollectionNames(JsonNode node, Set<String> names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            String value = node.asText("").trim();
            if (StringUtils.hasText(value)) {
                names.add(value);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectCollectionNames(item, names);
            }
            return;
        }
        if (node.isObject()) {
            String directName = textOf(node, "collectionName");
            if (StringUtils.hasText(directName)) {
                names.add(directName);
            }

            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                collectCollectionNames(iterator.next(), names);
            }
        }
    }

    private void collectQueryRows(JsonNode node, String primaryField, List<JsonNode> rows) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectQueryRows(item, primaryField, rows);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        boolean isRow = node.has(primaryField) || (!"id".equals(primaryField) && node.has("id"));
        if (isRow) {
            rows.add(node);
            return;
        }

        Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            collectQueryRows(iterator.next(), primaryField, rows);
        }
    }

    private boolean createCollection(RagProperties.MilvusProperties milvus, int dimension) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dbName", milvus.getDatabase());
        payload.put("collectionName", milvus.getCollection());
        payload.put("dimension", dimension);
        payload.put("metricType", "COSINE");
        payload.put("primaryFieldName", milvus.getPrimaryField());
        payload.put("idType", "Int64");
        payload.put("vectorFieldName", milvus.getVectorField());
        payload.put("enableDynamicField", true);

        return invokeMilvus(CREATE_COLLECTION_PATH, payload, true);
    }

    private boolean createIndexIfPossible(RagProperties.MilvusProperties milvus) {
        String indexName = milvus.getVectorField() + "_idx";

        Map<String, Object> payloadV2 = new LinkedHashMap<>();
        payloadV2.put("dbName", milvus.getDatabase());
        payloadV2.put("collectionName", milvus.getCollection());
        payloadV2.put("indexParams", List.of(Map.of(
                "fieldName", milvus.getVectorField(),
                "indexName", indexName,
                "metricType", "COSINE"
        )));

        if (invokeMilvus(CREATE_INDEX_PATH, payloadV2, false)) {
            return true;
        }

        Map<String, Object> payloadLegacy = new LinkedHashMap<>();
        payloadLegacy.put("dbName", milvus.getDatabase());
        payloadLegacy.put("collectionName", milvus.getCollection());
        payloadLegacy.put("fieldName", milvus.getVectorField());
        payloadLegacy.put("indexName", indexName);
        payloadLegacy.put("indexType", "AUTOINDEX");
        payloadLegacy.put("metricType", "COSINE");
        payloadLegacy.put("params", Map.of());

        boolean created = invokeMilvus(CREATE_INDEX_PATH, payloadLegacy, false);
        if (!created) {
            log.info("Milvus 索引创建已跳过（可能由 quick setup 自动处理）: collection={}", milvus.getCollection());
        }
        return created;
    }

    private boolean loadCollection(RagProperties.MilvusProperties milvus) {
        return invokeMilvus(
                LOAD_COLLECTION_PATH,
                Map.of(
                        "dbName", milvus.getDatabase(),
                        "collectionName", milvus.getCollection()
                ),
                true
        );
    }

    private boolean invokeMilvus(String path, Map<String, Object> payload, boolean logOnFailure) {
        return invokeMilvusForResult(path, payload, logOnFailure) != null;
    }

    private JsonNode invokeMilvusForResult(String path, Map<String, Object> payload, boolean logOnFailure) {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        String endpointBase = normalizeEndpoint(milvus.getEndpoint());
        if (!StringUtils.hasText(endpointBase)) {
            if (logOnFailure) {
                log.warn("Milvus endpoint 为空，无法调用 path={}", path);
            }
            return null;
        }

        String endpoint = endpointBase + path;
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            if (logOnFailure) {
                log.warn("Milvus 请求序列化失败: path={}, error={}", path, e.getMessage());
            }
            return null;
        }

        Exception lastError = null;
        for (int attempt = 1; attempt <= REQUEST_MAX_RETRIES; attempt++) {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(endpoint)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Connection", "close")
                        .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE));

                if (StringUtils.hasText(milvus.getToken())) {
                    requestBuilder.addHeader("Authorization", "Bearer " + milvus.getToken().trim());
                }

                try (Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
                    String responseBody = response.body() == null ? "" : response.body().string();
                    if (!response.isSuccessful()) {
                        if (logOnFailure) {
                            log.warn("Milvus 调用失败: path={}, attempt={}, status={}, body={}",
                                    path,
                                    attempt,
                                    response.code(),
                                    safeSnippet(responseBody));
                        }
                        return null;
                    }

                    JsonNode root = objectMapper.readTree(responseBody);
                    int code = root.path("code").asInt(0);
                    if (code != 0) {
                        if (logOnFailure) {
                            log.warn("Milvus 返回错误: path={}, attempt={}, code={}, message={}",
                                    path,
                                    attempt,
                                    code,
                                    root.path("message").asText(""));
                        }
                        return null;
                    }
                    return root;
                }
            } catch (Exception e) {
                lastError = e;
                if (attempt < REQUEST_MAX_RETRIES && shouldRetry(e)) {
                    sleepQuietly(REQUEST_RETRY_BACKOFF_MS * attempt);
                    continue;
                }
                break;
            }
        }

        if (logOnFailure && lastError != null) {
            log.warn("Milvus 调用异常: path={}, error={}", path, lastError.getMessage());
        }
        return null;
    }

    private boolean shouldRetry(Exception e) {
        if (e instanceof IOException) {
            return true;
        }
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("unexpected end of stream")
                || normalized.contains("connection reset")
                || normalized.contains("timeout")
                || normalized.contains("refused");
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String safeSnippet(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }

    private List<RagChunk> parseSearchResult(String body, int topK) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        int code = root.path("code").asInt(0);
        if (code != 0) {
            log.warn("Milvus 返回错误: code={}, message={}", code, root.path("message").asText());
            return List.of();
        }

        List<JsonNode> candidates = new ArrayList<>();
        collectCandidates(root.path("data"), candidates);

        List<RagChunk> chunks = new ArrayList<>();
        for (JsonNode candidate : candidates) {
            RagChunk chunk = toChunk(candidate);
            if (chunk != null) {
                chunks.add(chunk);
            }
            if (chunks.size() >= topK) {
                break;
            }
        }
        return chunks;
    }

    private void collectCandidates(JsonNode node, List<JsonNode> output) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                collectCandidates(item, output);
            }
            return;
        }

        if (node.isObject()) {
            boolean isCandidate = node.has("entity") ||
                    (node.has("content") && (node.has("score") || node.has("distance") || node.has("id")));
            if (isCandidate) {
                output.add(node);
            }

            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                collectCandidates(iterator.next(), output);
            }
        }
    }

    private RagChunk toChunk(JsonNode candidate) {
        JsonNode entityNode = candidate.path("entity");

        String content = firstNonBlank(
                textOf(entityNode, "content"),
                textOf(candidate, "content"),
                textOf(entityNode, "description"),
                textOf(candidate, "description")
        );
        if (!StringUtils.hasText(content)) {
            return null;
        }

        String title = firstNonBlank(
                textOf(entityNode, "title"),
                textOf(entityNode, "name"),
                textOf(candidate, "title"),
                textOf(candidate, "name"),
                "知识片段"
        );

        String source = firstNonBlank(
                textOf(entityNode, "source"),
                textOf(entityNode, "city"),
                textOf(entityNode, "category"),
                "milvus"
        );

        Double score = null;
        if (candidate.hasNonNull("score")) {
            score = candidate.path("score").asDouble();
        } else if (candidate.hasNonNull("distance")) {
            score = 1D - candidate.path("distance").asDouble();
        }

        return RagChunk.builder()
                .title(title)
                .source(source)
                .content(content)
                .score(score)
                .build();
    }

    private String textOf(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return "";
        }
        String value = node.path(field).asText("").trim();
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeEndpoint(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return "";
        }
        String normalized = endpoint.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
