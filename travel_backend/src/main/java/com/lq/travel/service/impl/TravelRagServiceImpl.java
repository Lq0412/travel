package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lq.travel.config.RagProperties;
import com.lq.travel.mapper.KnowledgeAttractionMapper;
import com.lq.travel.mapper.KnowledgeExperienceMapper;
import com.lq.travel.mapper.KnowledgeFoodMapper;
import com.lq.travel.model.dto.ai.RagChunk;
import com.lq.travel.model.entity.KnowledgeAttraction;
import com.lq.travel.model.entity.KnowledgeExperience;
import com.lq.travel.model.entity.KnowledgeFood;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.service.TravelRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 旅行 RAG 实现。
 * 优先使用 Milvus 检索，失败后回退到本地知识表的关键词召回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelRagServiceImpl implements TravelRagService {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[\\s,，。；;、/|]+");

    private final RagProperties ragProperties;
    private final MilvusRagClient milvusRagClient;
    private final KnowledgeAttractionMapper knowledgeAttractionMapper;
    private final KnowledgeFoodMapper knowledgeFoodMapper;
    private final KnowledgeExperienceMapper knowledgeExperienceMapper;

    @Override
    public String buildRagContext(String userQuery, String destinationHint, IntentType intent) {
        if (!ragProperties.isEnabled() || !StringUtils.hasText(userQuery)) {
            return "";
        }

        int topK = Math.max(1, ragProperties.getTopK());
        List<RagChunk> merged = new ArrayList<>();

        String retrievalQuery = buildRetrievalQuery(userQuery, destinationHint, intent);
        if (ragProperties.getMilvus().isEnabled()) {
            merged.addAll(milvusRagClient.search(retrievalQuery, topK));
        }

        if (merged.size() < topK) {
            merged.addAll(searchFromKnowledgeTable(retrievalQuery, destinationHint, topK));
        }

        List<RagChunk> finalChunks = deduplicateAndRank(merged, topK);
        if (finalChunks.isEmpty()) {
            return "";
        }

        log.info("RAG检索完成: intent={}, queryLen={}, chunks={}", intent, userQuery.length(), finalChunks.size());
        return formatAsPromptContext(finalChunks);
    }

    private String buildRetrievalQuery(String userQuery, String destinationHint, IntentType intent) {
        StringBuilder queryBuilder = new StringBuilder();
        if (StringUtils.hasText(destinationHint)) {
            queryBuilder.append(destinationHint.trim()).append(" ");
        }
        queryBuilder.append(userQuery.trim());
        if (intent == IntentType.ITINERARY_GENERATION) {
            queryBuilder.append(" 路线 交通 时间 预算");
        }
        return queryBuilder.toString().trim();
    }

    private List<RagChunk> searchFromKnowledgeTable(String query, String destinationHint, int topK) {
        List<String> tokens = extractKeywords(query, destinationHint);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<RagChunk> candidates = new ArrayList<>();
        candidates.addAll(searchAttractions(tokens, destinationHint));
        candidates.addAll(searchFoods(tokens, destinationHint));
        candidates.addAll(searchExperiences(tokens, destinationHint));

        return candidates.stream()
                .sorted(Comparator.comparingDouble(this::safeScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<RagChunk> searchAttractions(List<String> tokens, String destinationHint) {
        try {
            List<KnowledgeAttraction> rows = knowledgeAttractionMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeAttraction>()
                            .orderByDesc(KnowledgeAttraction::getRating)
                            .last("limit 200")
            );

            List<RagChunk> result = new ArrayList<>();
            for (KnowledgeAttraction row : rows) {
                double score = scoreAttraction(row, tokens, destinationHint);
                if (score <= 0.1D) {
                    continue;
                }
                result.add(toAttractionChunk(row, score));
            }
            return result;
        } catch (Exception e) {
            log.warn("本地景点知识库检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RagChunk> searchFoods(List<String> tokens, String destinationHint) {
        try {
            List<KnowledgeFood> rows = knowledgeFoodMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeFood>()
                            .last("limit 200")
            );

            List<RagChunk> result = new ArrayList<>();
            for (KnowledgeFood row : rows) {
                double score = scoreFood(row, tokens, destinationHint);
                if (score <= 0.1D) {
                    continue;
                }
                result.add(toFoodChunk(row, score));
            }
            return result;
        } catch (Exception e) {
            log.warn("本地美食知识库检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RagChunk> searchExperiences(List<String> tokens, String destinationHint) {
        try {
            List<KnowledgeExperience> rows = knowledgeExperienceMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeExperience>()
                            .orderByDesc(KnowledgeExperience::getUpdateTime)
                            .last("limit 200")
            );

            List<RagChunk> result = new ArrayList<>();
            for (KnowledgeExperience row : rows) {
                double score = scoreExperience(row, tokens, destinationHint);
                if (score <= 0.1D) {
                    continue;
                }
                result.add(toExperienceChunk(row, score));
            }
            return result;
        } catch (Exception e) {
            log.warn("本地经验知识库检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    private RagChunk toAttractionChunk(KnowledgeAttraction row, double score) {
        String content = String.format(
                Locale.ROOT,
                "景点:%s；简介:%s；地址:%s；门票:%s；开放时间:%s；特色:%s",
                defaultText(row.getName(), "未知景点"),
                defaultText(row.getDescription(), "暂无简介"),
                defaultText(row.getAddress(), "暂无地址"),
                defaultText(row.getTicketPrice(), "以景区公示为准"),
                defaultText(row.getOpeningHours(), "以景区公告为准"),
                joinList(row.getFeatures(), "暂无")
        );

        return RagChunk.builder()
                .title(defaultText(row.getName(), "景点"))
                .source("mysql-knowledge-attraction")
                .content(content)
                .score(score)
                .build();
    }

    private RagChunk toFoodChunk(KnowledgeFood row, double score) {
        String content = String.format(
                Locale.ROOT,
                "美食:%s；简介:%s；类别:%s；口味:%s；推荐地点:%s；价格区间:%s",
                defaultText(row.getName(), "未知美食"),
                defaultText(row.getDescription(), "暂无简介"),
                defaultText(row.getCategory(), "暂无"),
                defaultText(row.getTaste(), "暂无"),
                defaultText(row.getWhereToEat(), "暂无"),
                defaultText(row.getPriceRange(), "以门店实际为准")
        );

        return RagChunk.builder()
                .title(defaultText(row.getName(), "美食"))
                .source("mysql-knowledge-food")
                .content(content)
                .score(score)
                .build();
    }

    private RagChunk toExperienceChunk(KnowledgeExperience row, double score) {
        String content = String.format(
                Locale.ROOT,
                "经验:%s；平台:%s；标签:%s；内容:%s；链接:%s",
                defaultText(row.getTitle(), "经验补齐"),
                defaultText(row.getPlatform(), "unknown"),
                defaultText(row.getTags(), "暂无"),
                defaultText(row.getContent(), "暂无"),
                defaultText(row.getUrl(), "暂无")
        );

        return RagChunk.builder()
                .title(defaultText(row.getTitle(), "经验补齐"))
                .source("mysql-knowledge-experience")
                .content(content)
                .score(score)
                .build();
    }

    private double scoreAttraction(KnowledgeAttraction row, List<String> tokens, String destinationHint) {
        double score = 0D;
        score += hitScore(row.getName(), tokens, 2.8D);
        score += hitScore(row.getDescription(), tokens, 1.8D);
        score += hitScore(row.getAddress(), tokens, 1.6D);
        score += hitScore(row.getCategory(), tokens, 1.2D);
        score += hitScore(joinList(row.getTags(), ""), tokens, 1.2D);
        score += hitScore(joinList(row.getFeatures(), ""), tokens, 1.0D);

        if (StringUtils.hasText(destinationHint)) {
            score += hitScore(defaultText(row.getAddress(), "") + " " + defaultText(row.getName(), ""),
                    List.of(destinationHint.trim()), 2.0D);
        }

        if (row.getRating() != null) {
            score += row.getRating() / 5.0D;
        }
        return score;
    }

    private double scoreFood(KnowledgeFood row, List<String> tokens, String destinationHint) {
        double score = 0D;
        score += hitScore(row.getName(), tokens, 2.4D);
        score += hitScore(row.getDescription(), tokens, 1.8D);
        score += hitScore(row.getWhereToEat(), tokens, 1.5D);
        score += hitScore(row.getCategory(), tokens, 1.1D);
        score += hitScore(row.getTaste(), tokens, 1.1D);
        score += hitScore(joinList(row.getTags(), ""), tokens, 1.0D);

        if (StringUtils.hasText(destinationHint)) {
            score += hitScore(defaultText(row.getWhereToEat(), "") + " " + defaultText(row.getName(), ""),
                    List.of(destinationHint.trim()), 1.8D);
        }

        return score;
    }

    private double scoreExperience(KnowledgeExperience row, List<String> tokens, String destinationHint) {
        double score = 0D;
        score += hitScore(row.getTitle(), tokens, 2.2D);
        score += hitScore(row.getContent(), tokens, 1.8D);
        score += hitScore(row.getTags(), tokens, 1.4D);
        score += hitScore(row.getPlatform(), tokens, 0.8D);

        if (StringUtils.hasText(destinationHint)) {
            score += hitScore(defaultText(row.getTitle(), "") + " " + defaultText(row.getContent(), ""),
                    List.of(destinationHint.trim()), 1.8D);
        }

        return score;
    }

    private double hitScore(String text, List<String> tokens, double weight) {
        if (!StringUtils.hasText(text) || tokens.isEmpty()) {
            return 0D;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            if (normalized.contains(token.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        if (hits == 0) {
            return 0D;
        }
        return weight * hits;
    }

    private List<String> extractKeywords(String query, String destinationHint) {
        StringBuilder combined = new StringBuilder();
        if (StringUtils.hasText(destinationHint)) {
            combined.append(destinationHint).append(' ');
        }
        combined.append(defaultText(query, ""));

        String[] raw = TOKEN_SPLIT_PATTERN.split(combined.toString());
        List<String> tokens = new ArrayList<>();
        for (String item : raw) {
            String token = item.trim();
            if (token.length() < 2) {
                continue;
            }
            tokens.add(token);
            if (tokens.size() >= 16) {
                break;
            }
        }
        return tokens;
    }

    private List<RagChunk> deduplicateAndRank(List<RagChunk> chunks, int topK) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<String, RagChunk> deduplicated = new LinkedHashMap<>();
        for (RagChunk chunk : chunks) {
            if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
                continue;
            }

            String key = chunk.getContent().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
            RagChunk existed = deduplicated.get(key);
            if (existed == null || safeScore(chunk) > safeScore(existed)) {
                deduplicated.put(key, chunk);
            }
        }

        return deduplicated.values().stream()
                .sorted(Comparator.comparingDouble(this::safeScore).reversed())
                .limit(topK)
                .toList();
    }

    private String formatAsPromptContext(List<RagChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("【RAG检索参考】以下信息来自旅行知识库，请优先基于这些事实进行路线设计和说明。\n");
        int snippetMaxLength = Math.max(80, ragProperties.getSnippetMaxLength());

        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            sb.append(i + 1).append(". ")
                    .append(truncate(chunk.getContent(), snippetMaxLength));
            if (StringUtils.hasText(chunk.getSource())) {
                sb.append("（来源:").append(chunk.getSource()).append("）");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return defaultText(text, "");
        }
        return text.substring(0, maxLength) + "...";
    }

    private String joinList(List<String> values, String defaultValue) {
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        String joined = String.join("、", values);
        return StringUtils.hasText(joined) ? joined : defaultValue;
    }

    private String defaultText(String text, String defaultValue) {
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }

    private double safeScore(RagChunk chunk) {
        return chunk.getScore() == null ? 0D : chunk.getScore();
    }
}
