package com.lq.travel.util;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构化行程提取与标准化工具
 * 从大模型输出文本中提取 JSON，并规范为前端可直接消费的字段结构。
 */
@Slf4j
public final class StructuredItineraryExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper LENIENT_OBJECT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();
    private static final Pattern HOUR_PATTERN = Pattern.compile("(\\d{1,2})");
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json|JSON)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final List<String> DAILY_PLAN_FIELD_CANDIDATES = List.of("dailyPlans", "dailyPlan", "plans", "planList");
    private static final Set<String> ALLOWED_ACTIVITY_TYPES = Set.of("attraction", "transport", "rest", "meal");

    private StructuredItineraryExtractor() {
    }

    /**
     * 从原始文本中提取并标准化结构化行程 JSON。
     *
     * @param sourceText 原始文本
     * @return 标准化后的 JSON（可直接给前端解析）
     */
    public static Optional<String> extractAndNormalize(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return Optional.empty();
        }

        String candidate = findBestItineraryJson(sourceText);
        if (candidate.isBlank()) {
            log.warn("未找到可用的行程JSON片段，响应预览: {}", preview(sourceText));
            return Optional.empty();
        }

        Optional<String> normalized = normalizeItinerary(candidate);
        if (normalized.isEmpty()) {
            log.warn("检测到疑似行程JSON，但标准化失败，片段预览: {}", preview(candidate));
        }
        return normalized;
    }

    private static Optional<String> normalizeItinerary(String rawJson) {
        try {
            JsonNode root = parseJsonNode(rawJson);
            if (root == null) {
                return Optional.empty();
            }

            JsonNode itineraryNode = findItineraryNode(root);
            if (!itineraryNode.isObject()) {
                return Optional.empty();
            }

            ArrayNode normalizedDailyPlans = normalizeDailyPlans(dailyPlansOf(itineraryNode));
            if (normalizedDailyPlans.isEmpty()) {
                log.warn("提取到 JSON，但 dailyPlans 为空，跳过结构化输出");
                return Optional.empty();
            }

            ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
            String destination = firstNonBlank(
                    textOf(itineraryNode, "destination", ""),
                    textOf(root, "destination", "")
            );
            normalized.put("destination", destination);

            int days = intOf(itineraryNode, "days", intOf(root, "days", normalizedDailyPlans.size()));
            days = Math.max(days, normalizedDailyPlans.size());
            normalized.put("days", days);

            int budget = intOf(itineraryNode, "budget", intOf(root, "budget", 0));
            normalized.put("budget", Math.max(budget, 0));

            String theme = firstNonBlank(
                    textOf(itineraryNode, "theme", ""),
                    textOf(root, "theme", "")
            );
            normalized.put("theme", theme);
            normalized.set("dailyPlans", normalizedDailyPlans);

            int totalEstimatedCost = intOf(itineraryNode, "totalEstimatedCost", intOf(root, "totalEstimatedCost", -1));
            if (totalEstimatedCost < 0) {
                totalEstimatedCost = calculateTotalEstimatedCost(normalizedDailyPlans);
            }
            normalized.put("totalEstimatedCost", Math.max(totalEstimatedCost, 0));

            ArrayNode tips = normalizeStringArray(itineraryNode.path("tips"));
            if (tips.isEmpty()) {
                tips = normalizeStringArray(root.path("tips"));
            }
            normalized.set("tips", tips);

            return Optional.of(OBJECT_MAPPER.writeValueAsString(normalized));
        } catch (Exception e) {
            log.warn("结构化行程 JSON 标准化失败", e);
            return Optional.empty();
        }
    }

    private static JsonNode parseJsonNode(String rawJson) {
        String candidate = stripCodeFence(rawJson).trim();
        if (candidate.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(candidate);
        } catch (Exception ignore) {
            // 严格模式失败后，尝试常见修复 + 宽松解析。
        }

        String repaired = repairCommonJsonIssues(candidate);
        if (repaired.isBlank()) {
            return null;
        }

        try {
            return LENIENT_OBJECT_MAPPER.readTree(repaired);
        } catch (Exception e) {
            log.debug("宽松 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private static JsonNode findItineraryNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return MissingNode.getInstance();
        }

        if (node.isObject()) {
            if (hasDailyPlans(node)) {
                return node;
            }

            JsonNode itinerary = node.path("itinerary");
            if (itinerary.isObject() && hasDailyPlans(itinerary)) {
                return itinerary;
            }

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                JsonNode nested = findItineraryNode(fields.next().getValue());
                if (!nested.isMissingNode()) {
                    return nested;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode nested = findItineraryNode(item);
                if (!nested.isMissingNode()) {
                    return nested;
                }
            }
        }

        return MissingNode.getInstance();
    }

    private static boolean hasDailyPlans(JsonNode node) {
        for (String field : DAILY_PLAN_FIELD_CANDIDATES) {
            JsonNode value = node.path(field);
            if (value.isArray() || value.isObject()) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode dailyPlansOf(JsonNode node) {
        for (String field : DAILY_PLAN_FIELD_CANDIDATES) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return MissingNode.getInstance();
    }

    private static ArrayNode normalizeDailyPlans(JsonNode dailyPlansNode) {
        ArrayNode result = OBJECT_MAPPER.createArrayNode();

        if (dailyPlansNode.isTextual()) {
            JsonNode parsed = parseJsonNode(dailyPlansNode.asText());
            return parsed == null ? result : normalizeDailyPlans(parsed);
        }

        List<JsonNode> planNodes = new ArrayList<>();
        if (dailyPlansNode.isArray()) {
            dailyPlansNode.forEach(planNodes::add);
        } else if (dailyPlansNode.isObject()) {
            List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
            dailyPlansNode.fields().forEachRemaining(entries::add);
            entries.sort(Comparator.comparingInt(entry -> parseInt(entry.getKey(), Integer.MAX_VALUE)));
            for (Map.Entry<String, JsonNode> entry : entries) {
                planNodes.add(entry.getValue());
            }
        } else {
            return result;
        }

        int defaultDay = 1;
        for (JsonNode planNode : planNodes) {
            if (!planNode.isObject()) {
                continue;
            }

            ObjectNode normalizedPlan = OBJECT_MAPPER.createObjectNode();
            int day = intOf(planNode, "day", defaultDay);
            normalizedPlan.put("day", Math.max(day, defaultDay));

            String date = textOf(planNode, "date", "");
            if (!date.isBlank()) {
                normalizedPlan.put("date", date);
            }

            JsonNode activitiesNode = firstNode(planNode, "activities", "items", "attractions");
            ArrayNode normalizedActivities = normalizeActivities(activitiesNode);
            normalizedPlan.set("activities", normalizedActivities);

            ArrayNode normalizedMeals = normalizeMeals(firstNode(planNode, "meals"));
            if (!normalizedMeals.isEmpty()) {
                normalizedPlan.set("meals", normalizedMeals);
            }

            result.add(normalizedPlan);
            defaultDay++;
        }

        return result;
    }

    private static ArrayNode normalizeActivities(JsonNode activitiesNode) {
        ArrayNode result = OBJECT_MAPPER.createArrayNode();

        if (activitiesNode.isTextual()) {
            JsonNode parsed = parseJsonNode(activitiesNode.asText());
            return parsed == null ? result : normalizeActivities(parsed);
        }

        if (!activitiesNode.isArray()) {
            return result;
        }

        for (JsonNode activityNode : activitiesNode) {
            if (!activityNode.isObject()) {
                continue;
            }

            ObjectNode normalizedActivity = OBJECT_MAPPER.createObjectNode();
            normalizedActivity.put("time", normalizePeriod(textOf(activityNode, "time", "morning")));
            String activityName = firstNonBlank(
                    textOf(activityNode, "name", ""),
                    textOf(activityNode, "title", ""),
                    textOf(activityNode, "spot", ""),
                    "未命名景点"
            );
            normalizedActivity.put("name", activityName);
            normalizedActivity.put("type", normalizeActivityType(textOf(activityNode, "type", "attraction")));
            String description = firstNonBlank(
                    textOf(activityNode, "description", ""),
                    textOf(activityNode, "desc", ""),
                    textOf(activityNode, "content", "")
            );
            normalizedActivity.put("description", description);

            String imageUrl = firstNonBlank(
                    textOf(activityNode, "imageUrl", ""),
                    textOf(activityNode, "image", ""),
                    textOf(activityNode, "picture", "")
            );
            if (!imageUrl.isBlank()) {
                normalizedActivity.put("imageUrl", imageUrl);
            }

            ObjectNode location = normalizeLocation(activityNode.path("location"), textOf(activityNode, "address", ""));
            normalizedActivity.set("location", location);

            int estimatedCost = Math.max(intOf(activityNode, "estimatedCost", 0), 0);
            normalizedActivity.put("estimatedCost", estimatedCost);

            ArrayNode tips = normalizeStringArray(activityNode.path("tips"));
            if (!tips.isEmpty()) {
                normalizedActivity.set("tips", tips);
            }

            result.add(normalizedActivity);
        }

        return result;
    }

    private static ArrayNode normalizeMeals(JsonNode mealsNode) {
        ArrayNode result = OBJECT_MAPPER.createArrayNode();

        if (mealsNode.isTextual()) {
            JsonNode parsed = parseJsonNode(mealsNode.asText());
            return parsed == null ? result : normalizeMeals(parsed);
        }

        if (!mealsNode.isArray()) {
            return result;
        }

        for (JsonNode mealNode : mealsNode) {
            if (!mealNode.isObject()) {
                continue;
            }

            ObjectNode normalizedMeal = OBJECT_MAPPER.createObjectNode();
            normalizedMeal.put("time", textOf(mealNode, "time", ""));
            normalizedMeal.put("type", textOf(mealNode, "type", "lunch"));
            normalizedMeal.put("recommendation", textOf(mealNode, "recommendation", ""));
            normalizedMeal.put("estimatedCost", Math.max(intOf(mealNode, "estimatedCost", 0), 0));
            result.add(normalizedMeal);
        }

        return result;
    }

    private static ObjectNode normalizeLocation(JsonNode locationNode, String fallbackAddress) {
        ObjectNode normalizedLocation = OBJECT_MAPPER.createObjectNode();

        String address = fallbackAddress;
        if (locationNode.isObject()) {
            address = firstNonBlank(textOf(locationNode, "address", ""), fallbackAddress);
            JsonNode coordinates = locationNode.path("coordinates");
            if (coordinates.isArray() && coordinates.size() == 2
                    && coordinates.get(0).isNumber() && coordinates.get(1).isNumber()) {
                normalizedLocation.set("coordinates", coordinates.deepCopy());
            }
        }

        normalizedLocation.put("address", address == null ? "" : address);
        return normalizedLocation;
    }

    private static int calculateTotalEstimatedCost(ArrayNode dailyPlans) {
        int total = 0;
        for (JsonNode planNode : dailyPlans) {
            JsonNode activities = planNode.path("activities");
            if (!activities.isArray()) {
                continue;
            }
            for (JsonNode activityNode : activities) {
                total += Math.max(intOf(activityNode, "estimatedCost", 0), 0);
            }
        }
        return total;
    }

    private static ArrayNode normalizeStringArray(JsonNode node) {
        ArrayNode normalized = OBJECT_MAPPER.createArrayNode();

        if (node.isTextual()) {
            String[] parts = node.asText().split("[，,、;；\\n]+");
            for (String part : parts) {
                String cleaned = part == null ? "" : part.trim();
                if (!cleaned.isEmpty()) {
                    normalized.add(cleaned);
                }
            }
            return normalized;
        }

        if (!node.isArray()) {
            return normalized;
        }

        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                normalized.add(item.asText());
            }
        }
        return normalized;
    }

    private static String normalizePeriod(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();

        if (value.contains("morning") || value.contains("早") || value.contains("上午")) {
            return "morning";
        }
        if (value.contains("noon") || value.contains("afternoon") || value.contains("中") || value.contains("午")) {
            return "noon";
        }
        if (value.contains("evening") || value.contains("night") || value.contains("晚")) {
            return "evening";
        }

        Matcher matcher = HOUR_PATTERN.matcher(value);
        if (matcher.find()) {
            int hour = parseInt(matcher.group(1), 8);
            if (hour < 11) {
                return "morning";
            }
            if (hour < 17) {
                return "noon";
            }
            return "evening";
        }

        return "morning";
    }

    private static String normalizeActivityType(String rawType) {
        String type = rawType == null ? "" : rawType.trim().toLowerCase();
        if (ALLOWED_ACTIVITY_TYPES.contains(type)) {
            return type;
        }
        return "attraction";
    }

    private static String textOf(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return defaultValue;
        }

        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return defaultValue;
        }

        String value = valueNode.asText();
        return value == null ? defaultValue : value.trim();
    }

    private static int intOf(JsonNode node, String fieldName, int defaultValue) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return defaultValue;
        }

        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isNumber()) {
            return valueNode.asInt();
        }

        if (valueNode.isTextual()) {
            return parseInt(valueNode.asText(), defaultValue);
        }

        return defaultValue;
    }

    private static int parseInt(String text, int defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }

        String cleaned = text.replaceAll("[^0-9-]", "");
        if (cleaned.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static JsonNode firstNode(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null || fieldNames.length == 0) {
            return MissingNode.getInstance();
        }

        for (String fieldName : fieldNames) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }

            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                return valueNode;
            }
        }

        return MissingNode.getInstance();
    }

    private static String stripCodeFence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("(?i)```json", "")
                .replace("```", "")
                .trim();
    }

    private static String repairCommonJsonIssues(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String repaired = text
                .replace('“', '"')
                .replace('”', '"')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('：', ':')
                .replace('，', ',')
                .replace('（', '(')
                .replace('）', ')');

        repaired = repaired.replaceAll("(?m)//.*$", "");
        repaired = repaired.replaceAll(",\\s*([}\\]])", "$1");
        return repaired.trim();
    }

    private static String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private static String findBestItineraryJson(String sourceText) {
        int depth = 0;
        int startIndex = -1;
        boolean inString = false;
        boolean escaped = false;
        String bestCandidate = "";

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                if (depth == 0) {
                    startIndex = i;
                }
                depth++;
                continue;
            }

            if (current == '}') {
                if (depth == 0) {
                    continue;
                }

                depth--;
                if (depth == 0 && startIndex >= 0) {
                    String candidate = sourceText.substring(startIndex, i + 1);
                    if (looksLikeItinerary(candidate) && candidate.length() > bestCandidate.length()) {
                        bestCandidate = candidate;
                    }
                    startIndex = -1;
                }
            }
        }

        if (bestCandidate.isBlank()) {
            Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(sourceText);
            while (matcher.find()) {
                String block = matcher.group(1);
                if (looksLikeItinerary(block) && block.length() > bestCandidate.length()) {
                    bestCandidate = block;
                }
            }
        }

        return bestCandidate;
    }

    private static boolean looksLikeItinerary(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return false;
        }

        String compact = jsonText
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .replace(" ", "");

        String lower = compact.toLowerCase();
        boolean hasDailyPlans = lower.contains("\"dailyplans\"")
                || lower.contains("'dailyplans'")
                || lower.contains("dailyplans:")
                || lower.contains("\"dailyplan\"")
                || lower.contains("dailyplan:");

        boolean hasBasicFields = lower.contains("\"destination\"")
                || lower.contains("'destination'")
                || lower.contains("destination:")
                || lower.contains("\"days\"")
                || lower.contains("'days'")
                || lower.contains("days:");

        return hasDailyPlans && hasBasicFields;
    }
}
