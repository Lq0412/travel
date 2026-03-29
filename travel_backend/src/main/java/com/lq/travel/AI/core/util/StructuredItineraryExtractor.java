package com.lq.travel.AI.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

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
    private static final Pattern HOUR_PATTERN = Pattern.compile("(\\d{1,2})");
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
            return Optional.empty();
        }

        return normalizeItinerary(candidate);
    }

    private static Optional<String> normalizeItinerary(String rawJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);
            if (!root.isObject()) {
                return Optional.empty();
            }

            ArrayNode normalizedDailyPlans = normalizeDailyPlans(root.path("dailyPlans"));
            if (normalizedDailyPlans.isEmpty()) {
                log.warn("提取到 JSON，但 dailyPlans 为空，跳过结构化输出");
                return Optional.empty();
            }

            ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
            normalized.put("destination", textOf(root, "destination", ""));

            int days = intOf(root, "days", normalizedDailyPlans.size());
            days = Math.max(days, normalizedDailyPlans.size());
            normalized.put("days", days);

            normalized.put("budget", Math.max(intOf(root, "budget", 0), 0));
            normalized.put("theme", textOf(root, "theme", ""));
            normalized.set("dailyPlans", normalizedDailyPlans);

            int totalEstimatedCost = intOf(root, "totalEstimatedCost", -1);
            if (totalEstimatedCost < 0) {
                totalEstimatedCost = calculateTotalEstimatedCost(normalizedDailyPlans);
            }
            normalized.put("totalEstimatedCost", Math.max(totalEstimatedCost, 0));

            normalized.set("tips", normalizeStringArray(root.path("tips")));
            return Optional.of(OBJECT_MAPPER.writeValueAsString(normalized));
        } catch (Exception e) {
            log.warn("结构化行程 JSON 标准化失败", e);
            return Optional.empty();
        }
    }

    private static ArrayNode normalizeDailyPlans(JsonNode dailyPlansNode) {
        ArrayNode result = OBJECT_MAPPER.createArrayNode();
        if (!dailyPlansNode.isArray()) {
            return result;
        }

        int defaultDay = 1;
        for (JsonNode planNode : dailyPlansNode) {
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

            ArrayNode normalizedActivities = normalizeActivities(planNode.path("activities"));
            normalizedPlan.set("activities", normalizedActivities);

            ArrayNode normalizedMeals = normalizeMeals(planNode.path("meals"));
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
        if (!activitiesNode.isArray()) {
            return result;
        }

        for (JsonNode activityNode : activitiesNode) {
            if (!activityNode.isObject()) {
                continue;
            }

            ObjectNode normalizedActivity = OBJECT_MAPPER.createObjectNode();
            normalizedActivity.put("time", normalizePeriod(textOf(activityNode, "time", "morning")));
            normalizedActivity.put("name", textOf(activityNode, "name", "未命名景点"));
            normalizedActivity.put("type", normalizeActivityType(textOf(activityNode, "type", "attraction")));
            normalizedActivity.put("description", textOf(activityNode, "description", ""));

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
        if (valueNode.isInt() || valueNode.isLong()) {
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

        return compact.contains("\"dailyPlans\"")
                && (compact.contains("\"destination\"") || compact.contains("\"days\""));
    }
}
