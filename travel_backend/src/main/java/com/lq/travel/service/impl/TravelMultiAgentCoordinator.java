package com.lq.travel.service.impl;

import com.lq.travel.model.dto.ai.AgentRequest;
import com.lq.travel.model.enums.IntentType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多智能体协同编排器。
 * 通过角色分工方式增强单次大模型调用的稳定性与可执行性。
 */
@Service
public class TravelMultiAgentCoordinator {

    private static final Pattern DESTINATION_LABEL_PATTERN = Pattern.compile("(?:目的地|城市|地点)\\s*[:：]\\s*([\\p{IsHan}A-Za-z0-9·]{2,20})");
    private static final Pattern DESTINATION_VERB_PATTERN = Pattern.compile("(?:去|到|前往|想去|打算去)([\\p{IsHan}A-Za-z0-9·]{2,20})");

    /**
     * 从任务文本中推断目的地提示。
     */
    public String inferDestinationHint(AgentRequest request) {
        String fullText = mergeText(request);
        if (!StringUtils.hasText(fullText)) {
            return "";
        }

        Matcher labelMatcher = DESTINATION_LABEL_PATTERN.matcher(fullText);
        if (labelMatcher.find()) {
            return cleanDestination(labelMatcher.group(1));
        }

        Matcher verbMatcher = DESTINATION_VERB_PATTERN.matcher(fullText);
        if (verbMatcher.find()) {
            return cleanDestination(verbMatcher.group(1));
        }

        return "";
    }

    /**
     * 构建多智能体协同增强后的 Prompt。
     */
    public String buildCoordinatedPrompt(AgentRequest request, IntentType intent, String ragContext, String destinationHint) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("【协同策略】你将以多智能体协同方式完成任务，但最终只输出统一结论，不要暴露内部推理过程。\n")
                .append("- 需求分析Agent: 提炼硬约束（天数、预算、同行人群、偏好、避雷项）\n")
                .append("- 目的地知识Agent: 使用检索事实，不编造开放时间、票价、地理关系\n")
                .append("- 路线设计Agent: 先分区聚类再排序，减少折返，控制跨区移动\n")
                .append("- 质检Agent: 校验字段完整性、时段合理性、预算一致性\n\n");

        if (StringUtils.hasText(destinationHint)) {
            prompt.append("【目的地线索】\n")
                    .append(destinationHint)
                    .append("\n\n");
        }

        if (StringUtils.hasText(ragContext)) {
            prompt.append(ragContext).append("\n\n");
        }

        if (StringUtils.hasText(request.getContext())) {
            prompt.append("【上下文信息】\n").append(request.getContext()).append("\n\n");
        }

        prompt.append("【用户需求】\n").append(defaultText(request.getTask())).append("\n\n");

        if (StringUtils.hasText(request.getGoal())) {
            prompt.append("【目标】\n").append(request.getGoal()).append("\n\n");
        }

        if (StringUtils.hasText(request.getConstraints())) {
            prompt.append("【约束条件】\n").append(request.getConstraints()).append("\n\n");
        }

        if (intent == IntentType.ITINERARY_GENERATION) {
            prompt.append("【路线设计硬规则】\n")
                    .append("1. 同一天行程按地理邻近聚类，避免跨城区来回折返。\n")
                    .append("2. 每天最多安排1次跨区远距离移动，并优先放在 noon 时段。\n")
                    .append("3. morning/noon/evening 需体现动静搭配：高强度景点后接休整或餐饮。\n")
                    .append("4. 预算估算要可追溯到活动项，totalEstimatedCost 必须与活动级成本一致。\n")
                    .append("5. 如果信息不足，用 tips 标注待确认，不要编造不存在的景点坐标。\n\n");
        }

        prompt.append("【输出要求】\n")
                .append("请给出可直接执行的结果，语言简洁、具体、可落地。\n");

        return prompt.toString();
    }

    private String mergeText(AgentRequest request) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, request.getTask());
        appendIfPresent(sb, request.getContext());
        appendIfPresent(sb, request.getGoal());
        appendIfPresent(sb, request.getConstraints());
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(text.trim());
    }

    private String cleanDestination(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String normalized = raw.trim();
        Set<String> stopWords = new LinkedHashSet<>();
        stopWords.add("旅游");
        stopWords.add("旅行");
        stopWords.add("行程");
        stopWords.add("攻略");
        stopWords.add("计划");
        stopWords.add("方案");

        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String stop : stopWords) {
            if (lower.equals(stop)) {
                return "";
            }
        }
        return normalized;
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "";
    }
}
