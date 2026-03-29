package com.lq.travel.constant;

/**
 * 超时配置常量
 * 集中管理所有超时相关的配置参数
 */
public class TimeoutConfig {
    
    // ==================== SSE超时配置 ====================
    
    /**
     * AI流式响应超时时间（毫秒）
     * 5分钟，适用于复杂的AI推理任务
     */
    public static final long SSE_TIMEOUT_AI_STREAM = 300_000L;
    
    // ==================== 转换为秒 ====================
    
    /**
     * AI流式响应超时时间（秒）
     */
    public static final int SSE_TIMEOUT_AI_STREAM_SECONDS = 300;
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private TimeoutConfig() {
        throw new AssertionError("TimeoutConfig不应该被实例化");
    }
}


