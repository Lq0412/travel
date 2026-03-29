package com.lq.travel.AI.interfaces;

import com.lq.travel.model.dto.ai.AgentRequest;
import com.lq.travel.model.dto.ai.AgentResponse;

/**
 * 智能代理接口
 * 定义统一的代理行为接口
 */
public interface Agent {
    
    /**
     * 获取代理名称
     */
    String getName();
    
    /**
     * 获取代理描述
     */
    String getDescription();
    
    /**
     * 执行代理任务
     */
    AgentResponse execute(AgentRequest request);
    
    /**
     * 检查代理是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取代理能力列表
     */
    String[] getCapabilities();

    /**
     * 流式执行代理任务
     * 默认不支持，具体代理可覆盖提供流式能力
     */
    default void executeStream(AgentRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException("Streaming not supported");
    }

    /**
     * 流式执行代理任务（带会话ID，便于缓存与消息落库）
     */
    default void executeStream(AgentRequest request, Long conversationId, StreamCallback callback) {
        // 默认回退到不带会话ID的实现
        executeStream(request, callback);
    }
}
