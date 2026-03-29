package com.lq.travel.AI.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * AI响应模型
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AIResponse {
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 提供商名称
     */
    private String provider;
    
    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = true;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 使用的token数
     */
    private Integer tokensUsed;
    
    /**
     * 响应时间(毫秒)
     */
    private Long responseTime;
    
    /**
     * 对话标题（如果适用）
     */
    private String conversationTitle;
    
    /**
     * 扩展信息
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    /**
     * 是否来自缓存
     */
    @Builder.Default
    private Boolean fromCache = false;

    /**
     * 是否为缓存空值标记
     */
    @Builder.Default
    private boolean nullCacheMarker = false;

    @Builder.Default
    private boolean errorCacheMarker = false; // 错误缓存标记


    /**
     * 创建成功响应
     */
    public static AIResponse success(String content, String model, String provider) {
        return AIResponse.builder()
                .content(content)
                .model(model)
                .provider(provider)
                .success(true)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static AIResponse error(String errorMessage, String provider) {
        return AIResponse.builder()
                .errorMessage(errorMessage)
                .provider(provider)
                .success(false)
                .build();
    }
    /**
     * 创建错误缓存标记响应
     */
    public static AIResponse errorCacheMarker() {
        return AIResponse.builder()
                .errorCacheMarker(true)
                .success(false)
                .errorMessage("ERROR_CACHE_MARKER")
                .build();
    }

    /**
     * 创建空值缓存标记响应
     */
    public static AIResponse nullCacheMarker() {
        return AIResponse.builder()
                .nullCacheMarker(true)
                .success(false)
                .errorMessage("NULL_CACHE_MARKER")
                .build();
    }
}
