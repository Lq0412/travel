package com.lq.travel.AI.core.constants;

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
    
    /**
     * 数字人对话温度
     * 稍高的温度使对话更自然、更有人情味，适用于语音对话场景
     */
    public static final double TEMPERATURE_DIGITAL_HUMAN = 0.7;
    
    /**
     * ReAct代理温度
     * 平衡推理准确性和创造性
     */
    public static final double TEMPERATURE_REACT = 0.7;
    
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
    
    /**
     * 数字人响应最大Token数
     * 适合语音播报的长度，避免过长
     */
    public static final int MAX_TOKENS_DIGITAL_HUMAN = 1500;
    
    /**
     * ReAct代理最大Token数
     * 适用于推理和行动循环
     */
    public static final int MAX_TOKENS_REACT = 1000;
    
    // ==================== 历史消息配置 ====================
    
    /**
     * 保留的历史消息最大数量
     * 避免上下文过长影响性能，同时保持足够的对话连贯性
     */
    public static final int MAX_HISTORY_MESSAGES = 10;
    
    // ==================== Agent步骤配置 ====================
    
    /**
     * 默认最大步骤数
     * Agent执行任务时的默认步骤上限
     */
    public static final int MAX_STEPS_DEFAULT = 10;
    
    /**
     * 旅游代理推荐步骤数
     * 适用于旅游行程规划等场景
     */
    public static final int MAX_STEPS_TOURISM = 5;
    
    /**
     * 数字人对话步骤数
     * 减少步数以实现更快响应
     */
    public static final int MAX_STEPS_DIGITAL_HUMAN = 3;
    
    /**
     * 触发任务完成提示的步骤阈值
     * 当步骤数达到此值时，提示Agent输出最终结果
     */
    public static final int COMPLETION_STEP_THRESHOLD = 3;
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private AIModelConfig() {
        throw new AssertionError("AIModelConfig不应该被实例化");
    }
}

