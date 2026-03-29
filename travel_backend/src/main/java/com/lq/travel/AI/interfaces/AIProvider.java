package com.lq.travel.AI.interfaces;

import com.lq.travel.AI.model.AIRequest;
import com.lq.travel.AI.model.AIResponse;

/**
 * AI提供商接口
 * 定义统一的AI调用接口，支持不同的AI服务提供商
 */
public interface AIProvider {
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 检查提供商是否可用
     */
    boolean isAvailable();
    
    /**
     * 发送聊天请求
     */
    AIResponse chat(AIRequest request);
    
    /**
     * 流式聊天请求
     */
    void chatStream(AIRequest request, StreamCallback callback);
    
    /**
     * 获取模型列表
     */
    String[] getAvailableModels();
    
    /**
     * 获取默认模型
     */
    String getDefaultModel();
}
