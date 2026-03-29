package com.lq.travel.AI.util;

import com.lq.travel.AI.model.IntentType;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 意图识别工具类
 * 通过正则表达式匹配用户输入，识别用户的真实意图
 */
@Slf4j
public class IntentAnalyzer {
    
    // 行程规划关键词模式
    private static final Pattern ITINERARY_PATTERN = Pattern.compile(
        ".*(帮我|给我|想要|需要|请帮|麻烦).*(规划|安排|制定|设计|生成|做一份|出一份|一版).*(行程|计划|路线|方案)|" +
        ".*(生成|规划|安排|制定|设计).*(行程|计划|路线|方案)|" +
        ".*(几天|\\d+\\s*天|\\d+\\s*日).*(游|旅游|旅行|路线|行程)|" +
        ".*怎么玩.*|" +
        ".*(行程|路线|方案).*(推荐|建议).*|" +
        ".*(帮我做|做一份|出一份|一版).*(行程|路线|方案).*"
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
        
        // 优先匹配行程规划（更具体的意图）
        if (ITINERARY_PATTERN.matcher(input).find()) {
            log.info("识别为行程规划意图: {}", userInput);
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
