package com.lq.travel.constant;

/**
 * AI模型配置常量
 * 集中管理所有AI模型相关的配置参数
 */
public class AIModelConfig {
    
    // ==================== 温度参数配置 ====================
    
    /**
     * 标准对话温度
     * 较低的温度以确保信息准确性，适用于需要精确回答的场景
     */
    public static final double TEMPERATURE_DEFAULT = 0.6;
    
    // ==================== Token限制配置 ====================
    
    /**
     * 标准响应最大Token数
     * 适用于一般性对话和回答
     */
    public static final int MAX_TOKENS_DEFAULT = 1200;
    
    /**
     * 扩展响应最大Token数
     * 适用于流式输出和需要较长回答的场景
     */
    public static final int MAX_TOKENS_EXTENDED = 2000;
    
    // ==================== 历史消息配置 ====================
    
    /**
     * 保留的历史消息最大数量
     * 避免上下文过长影响性能，同时保持足够的对话连贯性
     */
    public static final int MAX_HISTORY_MESSAGES = 10;
    
    // ==================== Agent步骤配置 ====================
    
    /**
     * 旅游代理推荐步骤数
     * 适用于旅游行程规划等场景
     */
    public static final int MAX_STEPS_TOURISM = 5;
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private AIModelConfig() {
        throw new AssertionError("AIModelConfig不应该被实例化");
    }
}


