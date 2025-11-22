package com.lq.travel.AI.core.util;

import com.lq.travel.AI.core.constants.RegexPatterns;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

/**
 * AI响应解析器
 * 解析结构化的AI响应（思考、行动、观察）
 */
@Slf4j
public class ResponseParser {
    
    /**
     * 解析响应结果的容器类
     */
    public static class ParsedResponse {
        public String action;
        public String observation;
        public String reasoning;
        
        public ParsedResponse(String action, String observation, String reasoning) {
            this.action = action;
            this.observation = observation;
            this.reasoning = reasoning;
        }
    }
    
    /**
     * 解析AI的结构化响应
     * 
     * @param response AI原始响应文本
     * @return 解析后的结构化结果
     */
    public static ParsedResponse parse(String response) {
        // 默认值
        String action = "规划";
        String observation = response;
        String reasoning = "基于用户需求进行旅游规划";
        
        try {
            Matcher matcher = RegexPatterns.RESPONSE_PARSE.matcher(response.trim());
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    reasoning = matcher.group(1).trim();
                }
                if (matcher.group(2) != null) {
                    action = matcher.group(2).trim();
                }
                if (matcher.group(3) != null) {
                    observation = matcher.group(3).trim();
                }
            }
        } catch (Exception e) {
            log.warn("响应解析失败: {}", e.getMessage());
        }
        
        return new ParsedResponse(action, observation, reasoning);
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private ResponseParser() {
        throw new AssertionError("ResponseParser不应该被实例化");
    }
}

