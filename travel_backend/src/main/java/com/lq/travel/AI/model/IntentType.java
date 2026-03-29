package com.lq.travel.AI.model;

/**
 * 用户意图类型枚举
 * 用于识别用户的对话意图，以便提供更精准的服务
 */
public enum IntentType {
    /**
     * 行程规划意图
     * 用户希望生成完整的旅行行程计划
     */
    ITINERARY_GENERATION("行程规划"),
    
    /**
     * 景点查询意图
     * 用户想了解具体景点的信息
     */
    ATTRACTION_QUERY("景点查询"),
    
    /**
     * 通用聊天意图
     * 普通对话或其他类型的咨询
     */
    GENERAL_CHAT("通用聊天");
    
    private final String description;
    
    IntentType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
