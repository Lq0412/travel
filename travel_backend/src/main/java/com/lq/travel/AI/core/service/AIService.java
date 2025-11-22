package com.lq.travel.AI.core.service;


import com.lq.travel.AI.core.interfaces.StreamCallback;
import com.lq.travel.AI.core.model.AIRequest;
import com.lq.travel.AI.core.model.AIResponse;

import java.util.Map;

/**
 * AI服务接口
 * 提供统一的AI调用服务
 */
public interface AIService {
    
    /**
     * 发送聊天请求
     */
    AIResponse chat(AIRequest request);
    
    /**
     * 发送聊天请求（支持对话历史）
     */
    AIResponse chat(AIRequest request, Long conversationId);
    
    /**
     * 流式聊天请求
     */
    void chatStream(AIRequest request, StreamCallback callback);
    
    /**
     * 流式聊天请求（支持对话历史）
     */
    void chatStream(AIRequest request, Long conversationId, StreamCallback callback);
    
    /**
     * 使用指定提供商发送请求
     */
    AIResponse chatWithProvider(String providerName, AIRequest request);
    
    /**
     * 使用指定提供商发送请求（支持对话历史）
     */
    AIResponse chatWithProvider(String providerName, AIRequest request, Long conversationId);
    
    /**
     * 获取可用的AI提供商列表
     */
    String[] getAvailableProviders();
    
    /**
     * 获取默认提供商
     */
    String getDefaultProvider();
    
    /**
     * 设置默认提供商
     */
    void setDefaultProvider(String providerName);
    
    /**
     * 获取可用的AI模型列表
     */
    String[] getAvailableModels();
    
    /**
     * 获取指定提供商的可用模型列表
     */
    String[] getModelsByProvider(String providerName);
    
    /**
     * 获取当前默认模型
     */
    String getCurrentModel();
    
    /**
     * 设置默认模型
     */
    void setDefaultModel(String modelName);
    
    /**
     * 获取缓存统计信息
     */
    Map<String, Object> getCacheStats();
    
    /**
     * 清除对话缓存
     */
    void clearConversationCache(Long conversationId);
}
