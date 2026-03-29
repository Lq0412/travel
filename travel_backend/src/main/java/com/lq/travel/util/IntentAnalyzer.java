package com.lq.travel.util;

import com.lq.travel.model.enums.IntentType;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 意图识别工具类
 * 通过正则表达式匹配用户输入，识别用户的真实意图
 */
@Slf4j
public class IntentAnalyzer {
    private static final Pattern DIRECT_ITINERARY_PATTERN = Pattern.compile(
        "(旅游规划|旅行规划|旅游计划|旅行计划|行程规划|路线规划|行程安排|旅游攻略|旅行攻略)"
    );

    private static final Pattern PLAN_VERB_PATTERN = Pattern.compile(
        "(规划|安排|制定|设计|生成|做一份|出一份|做个|策划|定制)"
    );

    private static final Pattern PLAN_NOUN_PATTERN = Pattern.compile(
        "(行程|计划|路线|方案|攻略)"
    );

    private static final Pattern TRAVEL_PATTERN = Pattern.compile(
        "(游玩|旅游|旅行|出游|出行|度假|去哪玩|怎么玩)"
    );

    private static final Pattern DAY_PATTERN = Pattern.compile(
        "(几天|\\d+\\s*[天日]|[一二三四五六七八九十两]+\\s*[天日])"
    );

    private static final Pattern STRUCTURED_JSON_PATTERN = Pattern.compile(
        "(结构化行程|结构化\\s*json|输出要求|dailyplans|totalestimatedcost|imageurl)"
    );
    
    // 景点查询关键词模式
    private static final Pattern ATTRACTION_PATTERN = Pattern.compile(
        ".*(推荐|介绍|有什么|哪里|哪些).*(景点|地方|好玩)|" +
        ".*(景点|温泉|森林公园|古村|溪头村|石门).*(怎么样|如何|介绍)|" +
        ".*(门票|开放时间|交通).*"
    );
    
    /**
     * 分析用户输入，识别意图
     * 
     * @param userInput 用户输入的文本
     * @return 识别出的意图类型
     */
    public static IntentType analyze(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.GENERAL_CHAT;
        }
        
        String input = userInput.toLowerCase();
        String itineraryReason = matchItineraryReason(input);
        
        // 优先匹配行程规划（更具体的意图）
        if (itineraryReason != null) {
            log.info("识别为行程规划意图(规则: {}): {}", itineraryReason, userInput);
            return IntentType.ITINERARY_GENERATION;
        }
        
        // 其次匹配景点查询
        if (ATTRACTION_PATTERN.matcher(input).find()) {
            log.info("识别为景点查询意图: {}", userInput);
            return IntentType.ATTRACTION_QUERY;
        }
        
        // 默认通用聊天
        log.info("识别为通用聊天意图: {}", userInput);
        return IntentType.GENERAL_CHAT;
    }

    private static String matchItineraryReason(String input) {
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
    
    /**
     * 判断是否为行程规划意图
     */
    public static boolean isItineraryIntent(String userInput) {
        return analyze(userInput) == IntentType.ITINERARY_GENERATION;
    }
    
    /**
     * 判断是否为景点查询意图
     */
    public static boolean isAttractionIntent(String userInput) {
        return analyze(userInput) == IntentType.ATTRACTION_QUERY;
    }
}
