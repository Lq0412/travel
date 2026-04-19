package com.lq.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class IntentAnalyzer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CLASSIFICATION_PROMPT = """
            你是一个旅游对话意图分类器。根据用户消息判断意图，只返回JSON：{"intent": "ITINERARY_GENERATION"} 或 {"intent": "ATTRACTION_QUERY"} 或 {"intent": "GENERAL_CHAT"}

            规则：
            - ITINERARY_GENERATION：用户明确要求生成、规划、制定、安排一份完整的旅行行程计划。必须包含明确的动作词（规划/安排/制定/生成/做个/做一份）加行程名词（行程/计划/攻略/路线）。
            - ATTRACTION_QUERY：用户询问某个具体景点的介绍、门票、推荐。
            - GENERAL_CHAT：所有其他对话。包括但不限于：美食推荐、住宿咨询、交通问路、旅游闲聊、信息收集阶段的对话、带目的地的咨询（如"去汕头玩两天"）。

            判断要点：
            1. 推荐/咨询/问答类一律GENERAL_CHAT，包括美食推荐、商品推荐、旅行建议。
            2. 只有用户明确要求"生成/规划一份行程"时才是ITINERARY_GENERATION。
            3. 消息中包含目的地、天数、行程等上下文信息不代表要生成行程。
            """;

    private static final Pattern DIRECT_ITINERARY_PATTERN = Pattern.compile(
            "(旅游规划|旅行规划|旅游计划|旅行计划|行程规划|路线规划|行程安排|旅游攻略|旅行攻略)"
    );
    private static final Pattern PLAN_VERB_PATTERN = Pattern.compile("(规划|安排|制定|设计|生成|做一份|出一份|做个|策划|定制)");
    private static final Pattern PLAN_NOUN_PATTERN = Pattern.compile("(行程|计划|路线|方案|攻略)");
    private static final Pattern TRAVEL_PATTERN = Pattern.compile("(游玩|旅游|旅行|出游|出行|度假|去哪玩|怎么玩)");
    private static final Pattern DAY_PATTERN = Pattern.compile("(几天|\\d+\\s*[天日]|[一二三四五六七八九十两]+\\s*[天日])");
    private static final Pattern STRUCTURED_JSON_PATTERN = Pattern.compile(
            "(结构化行程|结构化\\s*json|输出要求|dailyplans|totalestimatedcost|imageurl)"
    );
    private static final Pattern ATTRACTION_PATTERN = Pattern.compile(
            ".*(推荐|介绍|有什么|哪里|哪些).*(景点|地方|好玩)|" +
            ".*(景点|温泉|森林公园|古村|溪头村|石门).*(怎么样|如何|介绍)|" +
            ".*(门票|开放时间|交通).*"
    );

    private final AIService aiService;

    public IntentAnalyzer(AIService aiService) {
        this.aiService = aiService;
    }

    public IntentType analyze(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.GENERAL_CHAT;
        }

        try {
            return analyzeByLLM(userInput);
        } catch (Exception e) {
            log.warn("LLM意图分类失败，回退到正则: {}", e.getMessage());
            return analyzeByRegex(userInput);
        }
    }

    private IntentType analyzeByLLM(String userInput) {
        AIRequest request = AIRequest.builder()
                .message(userInput)
                .systemPrompt(CLASSIFICATION_PROMPT)
                .temperature(0.0)
                .maxTokens(50)
                .build();

        AIResponse response = aiService.chat(request);
        if (!response.getSuccess() || response.getContent() == null) {
            throw new RuntimeException("LLM返回失败: " + response.getErrorMessage());
        }

        IntentType result = parseIntentFromJson(response.getContent().trim());
        if (result == null) {
            throw new RuntimeException("LLM返回的意图无法解析");
        }
        return result;
    }

    private IntentType parseIntentFromJson(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String intentStr = node.has("intent") ? node.get("intent").asText() : "";
            return switch (intentStr) {
                case "ITINERARY_GENERATION" -> IntentType.ITINERARY_GENERATION;
                case "ATTRACTION_QUERY" -> IntentType.ATTRACTION_QUERY;
                case "GENERAL_CHAT" -> IntentType.GENERAL_CHAT;
                default -> {
                    log.warn("LLM返回未知意图: {}", intentStr);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("意图JSON解析失败: {}", json);
            return null;
        }
    }

    IntentType analyzeByRegex(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.GENERAL_CHAT;
        }

        String input = userInput.toLowerCase();
        String itineraryReason = matchItineraryReason(input);

        if (itineraryReason != null) {
            log.info("正则回退-识别为行程规划意图(规则: {}): {}", itineraryReason, userInput);
            return IntentType.ITINERARY_GENERATION;
        }

        if (ATTRACTION_PATTERN.matcher(input).find()) {
            log.info("正则回退-识别为景点查询意图: {}", userInput);
            return IntentType.ATTRACTION_QUERY;
        }

        log.info("正则回退-识别为通用聊天意图: {}", userInput);
        return IntentType.GENERAL_CHAT;
    }

    private String matchItineraryReason(String input) {
        if (DIRECT_ITINERARY_PATTERN.matcher(input).find()) {
            return "direct_itinerary_keywords";
        }
        boolean hasPlanVerb = PLAN_VERB_PATTERN.matcher(input).find();
        boolean hasPlanNoun = PLAN_NOUN_PATTERN.matcher(input).find();
        boolean hasTravelWord = TRAVEL_PATTERN.matcher(input).find();
        boolean hasDayExpression = DAY_PATTERN.matcher(input).find();
        boolean hasStructuredJsonHint = STRUCTURED_JSON_PATTERN.matcher(input).find();

        if (hasStructuredJsonHint && (hasTravelWord || hasPlanNoun || hasPlanVerb)) {
            return "structured_json_requirement";
        }
        if (hasPlanVerb && (hasPlanNoun || hasTravelWord)) {
            return "plan_verb_with_plan_or_travel";
        }
        if (hasDayExpression && (hasTravelWord || hasPlanNoun || hasPlanVerb)) {
            return "day_expression_with_travel_context";
        }
        if (input.contains("怎么玩")) {
            return "how_to_play";
        }
        return null;
    }
}
