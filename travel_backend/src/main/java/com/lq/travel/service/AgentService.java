package com.lq.travel.service;

import com.lq.travel.agent.Agent;
import com.lq.travel.model.dto.ai.AgentRequest;
import com.lq.travel.model.dto.ai.AgentResponse;

/**
 * 代理服务接口
 * 提供统一的代理管理服务
 */
public interface AgentService {
    
    /**
     * 执行代理任务
     */
    AgentResponse executeAgent(String agentName, AgentRequest request);
    
    /**
     * 注册代理
     */
    void registerAgent(String name, Agent agent);
    
    /**
     * 获取可用的代理列表
     */
    String[] getAvailableAgents();
    
    /**
     * 获取代理信息
     */
    Agent getAgent(String name);
    
    /**
     * 检查代理是否存在
     */
    boolean hasAgent(String name);

    /**
     * 流式执行代理任务
     */
    void executeAgentStream(String agentName, AgentRequest request, com.lq.travel.callback.StreamCallback callback);

    /**
     * 流式执行代理任务（带会话ID），便于缓存与聊天历史保存
     */
    void executeAgentStream(String agentName, AgentRequest request, Long conversationId, com.lq.travel.callback.StreamCallback callback);
}
