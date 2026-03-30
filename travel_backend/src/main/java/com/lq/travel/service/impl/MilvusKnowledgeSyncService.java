package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lq.travel.config.RagProperties;
import com.lq.travel.mapper.KnowledgeAttractionMapper;
import com.lq.travel.mapper.KnowledgeFoodMapper;
import com.lq.travel.model.entity.KnowledgeAttraction;
import com.lq.travel.model.entity.KnowledgeFood;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Milvus 知识库初始化与全量同步服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusKnowledgeSyncService {

    private static final long ATTRACTION_DOC_ID_OFFSET = 1_000_000_000L;
    private static final long FOOD_DOC_ID_OFFSET = 2_000_000_000L;

    private final RagProperties ragProperties;
    private final DashScopeEmbeddingService embeddingService;
    private final MilvusRagClient milvusRagClient;
    private final KnowledgeAttractionMapper knowledgeAttractionMapper;
    private final KnowledgeFoodMapper knowledgeFoodMapper;

    /**
     * 启动时按配置执行自动初始化或自动同步。
     */
    public void autoSyncOnStartupIfEnabled() {
        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        if (!ragProperties.isEnabled() || !milvus.isEnabled()) {
            return;
        }

        int retryTimes = Math.max(1, milvus.getStartupInitRetryTimes());
        long retryIntervalMs = Math.max(500L, milvus.getStartupInitRetryIntervalMs());

        if (milvus.isAutoSyncOnStartup()) {
            for (int attempt = 1; attempt <= retryTimes; attempt++) {
                Map<String, Object> summary = syncKnowledgeToMilvus(false);
                String status = String.valueOf(summary.getOrDefault("status", "failed"));
                boolean ok = "success".equalsIgnoreCase(status) || "partial".equalsIgnoreCase(status);
                if (ok) {
                    log.info("Milvus 启动自动同步完成: attempt={}, summary={}", attempt, summary);
                    return;
                }

                if (attempt < retryTimes) {
                    log.warn("Milvus 启动自动同步未完成，将重试: attempt={}, summary={}", attempt, summary);
                    sleepQuietly(retryIntervalMs);
                } else {
                    log.warn("Milvus 启动自动同步失败，已达到最大重试次数: attempts={}, lastSummary={}", retryTimes, summary);
                }
            }
            return;
        }

        if (milvus.isAutoInitCollection()) {
            List<Float> probeVector = embeddingService.embed("旅行知识库初始化探针文本");
            if (probeVector.isEmpty()) {
                log.warn("Milvus 自动初始化跳过：无法生成探针向量");
                return;
            }

            for (int attempt = 1; attempt <= retryTimes; attempt++) {
                boolean ready = milvusRagClient.ensureCollectionReady(probeVector.size(), false);
                if (ready) {
                    log.info("Milvus 自动初始化结果: ready=true, attempt={}, collection={}",
                            attempt,
                            milvus.getCollection());
                    return;
                }

                if (attempt < retryTimes) {
                    log.warn("Milvus 自动初始化失败，准备重试: attempt={}, nextWaitMs={}, collection={}",
                            attempt,
                            retryIntervalMs,
                            milvus.getCollection());
                    sleepQuietly(retryIntervalMs);
                } else {
                    log.warn("Milvus 自动初始化失败，已达到最大重试次数: attempts={}, collection={}",
                            retryTimes,
                            milvus.getCollection());
                }
            }
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 全量同步景点/美食知识到 Milvus。
     */
    public Map<String, Object> syncKnowledgeToMilvus(boolean recreateCollection) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "skipped");
        summary.put("recreateCollection", recreateCollection);

        RagProperties.MilvusProperties milvus = ragProperties.getMilvus();
        if (!ragProperties.isEnabled() || !milvus.isEnabled()) {
            summary.put("reason", "rag_or_milvus_disabled");
            return summary;
        }

        int limitPerTable = Math.max(1, milvus.getSyncLimitPerTable());
        List<KnowledgeAttraction> attractions = loadAttractions(limitPerTable);
        List<KnowledgeFood> foods = loadFoods(limitPerTable);

        summary.put("collection", milvus.getCollection());
        summary.put("vectorField", milvus.getVectorField());
        summary.put("attractionRows", attractions.size());
        summary.put("foodRows", foods.size());

        if (attractions.isEmpty() && foods.isEmpty()) {
            summary.put("reason", "knowledge_tables_empty");
            return summary;
        }

        String probeText = pickProbeText(attractions, foods);
        List<Float> probeVector = embeddingService.embed(probeText);
        if (probeVector.isEmpty()) {
            summary.put("status", "failed");
            summary.put("reason", "embedding_probe_failed");
            return summary;
        }

        int dimension = probeVector.size();
        summary.put("embeddingDimension", dimension);
        boolean collectionReady = milvusRagClient.ensureCollectionReady(dimension, recreateCollection);
        if (!collectionReady) {
            summary.put("status", "failed");
            summary.put("reason", "collection_init_failed");
            return summary;
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        int embeddingFailures = 0;

        for (KnowledgeAttraction row : attractions) {
            Map<String, Object> doc = buildAttractionDoc(row, milvus.getVectorField());
            if (doc == null) {
                embeddingFailures++;
                continue;
            }
            documents.add(doc);
        }

        for (KnowledgeFood row : foods) {
            Map<String, Object> doc = buildFoodDoc(row, milvus.getVectorField());
            if (doc == null) {
                embeddingFailures++;
                continue;
            }
            documents.add(doc);
        }

        summary.put("preparedDocs", documents.size());
        summary.put("embeddingFailures", embeddingFailures);

        if (documents.isEmpty()) {
            summary.put("status", "failed");
            summary.put("reason", "no_document_embedded");
            return summary;
        }

        int batchSize = Math.max(1, milvus.getSyncBatchSize());
        int upserted = 0;
        int failedBatches = 0;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Map<String, Object>> batch = documents.subList(i, end);
            boolean ok = milvusRagClient.upsert(batch);
            if (ok) {
                upserted += batch.size();
            } else {
                failedBatches++;
            }
        }

        summary.put("batchSize", batchSize);
        summary.put("upsertedDocs", upserted);
        summary.put("failedBatches", failedBatches);
        summary.put("loaded", milvusRagClient.loadCollection());

        if (upserted == 0) {
            summary.put("status", "failed");
            summary.put("reason", "upsert_failed");
            return summary;
        }

        summary.put("status", failedBatches == 0 ? "success" : "partial");
        return summary;
    }

    private List<KnowledgeAttraction> loadAttractions(int limit) {
        return knowledgeAttractionMapper.selectList(
                new LambdaQueryWrapper<KnowledgeAttraction>()
                        .orderByAsc(KnowledgeAttraction::getId)
                        .last("limit " + limit)
        );
    }

    private List<KnowledgeFood> loadFoods(int limit) {
        return knowledgeFoodMapper.selectList(
                new LambdaQueryWrapper<KnowledgeFood>()
                        .orderByAsc(KnowledgeFood::getId)
                        .last("limit " + limit)
        );
    }

    private String pickProbeText(List<KnowledgeAttraction> attractions, List<KnowledgeFood> foods) {
        if (!attractions.isEmpty()) {
            return buildAttractionContent(attractions.get(0));
        }
        if (!foods.isEmpty()) {
            return buildFoodContent(foods.get(0));
        }
        return "旅行知识库初始化探针文本";
    }

    private Map<String, Object> buildAttractionDoc(KnowledgeAttraction row, String vectorField) {
        if (row == null || row.getId() == null) {
            return null;
        }

        String content = buildAttractionContent(row);
        List<Float> vector = embeddingService.embed(content);
        if (vector.isEmpty()) {
            return null;
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", ATTRACTION_DOC_ID_OFFSET + row.getId());
        doc.put(vectorField, vector);
        doc.put("title", defaultText(row.getName(), "景点"));
        doc.put("name", defaultText(row.getName(), "景点"));
        doc.put("content", content);
        doc.put("source", "knowledge_attraction");
        doc.put("city", extractCity(row.getAddress()));
        doc.put("category", defaultText(row.getCategory(), "景点"));
        doc.put("kbType", "attraction");
        doc.put("updatedAt", row.getUpdatedAt() == null ? null : row.getUpdatedAt().getTime());
        return doc;
    }

    private Map<String, Object> buildFoodDoc(KnowledgeFood row, String vectorField) {
        if (row == null || row.getId() == null) {
            return null;
        }

        String content = buildFoodContent(row);
        List<Float> vector = embeddingService.embed(content);
        if (vector.isEmpty()) {
            return null;
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", FOOD_DOC_ID_OFFSET + row.getId());
        doc.put(vectorField, vector);
        doc.put("title", defaultText(row.getName(), "美食"));
        doc.put("name", defaultText(row.getName(), "美食"));
        doc.put("content", content);
        doc.put("source", "knowledge_food");
        doc.put("city", extractCity(row.getWhereToEat()));
        doc.put("category", defaultText(row.getCategory(), "美食"));
        doc.put("kbType", "food");
        return doc;
    }

    private String buildAttractionContent(KnowledgeAttraction row) {
        return String.format(
                Locale.ROOT,
                "景点:%s；简介:%s；地址:%s；门票:%s；开放时间:%s；标签:%s；特色:%s",
                defaultText(row.getName(), "未知景点"),
                defaultText(row.getDescription(), "暂无简介"),
                defaultText(row.getAddress(), "暂无地址"),
                defaultText(row.getTicketPrice(), "以景区公示为准"),
                defaultText(row.getOpeningHours(), "以景区公告为准"),
                joinList(row.getTags()),
                joinList(row.getFeatures())
        );
    }

    private String buildFoodContent(KnowledgeFood row) {
        return String.format(
                Locale.ROOT,
                "美食:%s；简介:%s；类别:%s；口味:%s；推荐地点:%s；价格区间:%s；标签:%s",
                defaultText(row.getName(), "未知美食"),
                defaultText(row.getDescription(), "暂无简介"),
                defaultText(row.getCategory(), "暂无"),
                defaultText(row.getTaste(), "暂无"),
                defaultText(row.getWhereToEat(), "暂无"),
                defaultText(row.getPriceRange(), "以门店实际为准"),
                joinList(row.getTags())
        );
    }

    private String extractCity(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        int cityPos = normalized.indexOf('市');
        if (cityPos > 0 && cityPos <= 6) {
            return normalized.substring(0, cityPos + 1);
        }
        return normalized.length() <= 12 ? normalized : normalized.substring(0, 12);
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "暂无";
        }
        return String.join("、", list);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
