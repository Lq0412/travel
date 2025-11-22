package com.lq.travel.AI.core.service.impl;

import com.lq.travel.AI.core.interfaces.Agent;
import com.lq.travel.AI.core.interfaces.StreamCallback;
import com.lq.travel.AI.core.model.AgentRequest;
import com.lq.travel.AI.core.model.AgentResponse;
import com.lq.travel.AI.core.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理服务实现类
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {
    
    private final Map<String, Agent> agentMap = new ConcurrentHashMap<>();
    
    @Override
    public AgentResponse executeAgent(String agentName, AgentRequest request) {
        Agent agent = agentMap.get(agentName);
        if (agent == null) {
            return AgentResponse.error("代理不存在: " + agentName);
        }
        
        if (!agent.isAvailable()) {
            return AgentResponse.error("代理不可用: " + agentName);
        }
        
        return agent.execute(request);
    }
    
    @Override
    public void registerAgent(String name, Agent agent) {
        agentMap.put(name, agent);
        log.info("注册代理: {} - {}", name, agent.getDescription());
    }
    
    @Override
    public String[] getAvailableAgents() {
        return agentMap.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }
    
    @Override
    public Agent getAgent(String name) {
        return agentMap.get(name);
    }
    
    @Override
    public boolean hasAgent(String name) {
        return agentMap.containsKey(name);
    }

    @Override
    public void executeAgentStream(String agentName, AgentRequest request, StreamCallback callback) {
        Agent agent = agentMap.get(agentName);
        if (agent == null) {
            callback.onError(new IllegalArgumentException("代理不存在: " + agentName));
            return;
        }
        if (!agent.isAvailable()) {
            callback.onError(new IllegalStateException("代理不可用: " + agentName));
            return;
        }
        try {
            agent.executeStream(request, callback);
        } catch (UnsupportedOperationException e) {
            callback.onError(new IllegalStateException("该代理未实现流式能力: " + agentName, e));
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void executeAgentStream(String agentName, AgentRequest request, Long conversationId, StreamCallback callback) {
        Agent agent = agentMap.get(agentName);
        if (agent == null) {
            callback.onError(new IllegalArgumentException("代理不存在: " + agentName));
            return;
        }
        if (!agent.isAvailable()) {
            callback.onError(new IllegalStateException("代理不可用: " + agentName));
            return;
        }
        try {
            agent.executeStream(request, conversationId, callback);
        } catch (UnsupportedOperationException e) {
            // 回退到无会话ID的流式实现
            try {
                agent.executeStream(request, callback);
            } catch (Exception ex) {
                callback.onError(new IllegalStateException("该代理未实现流式能力: " + agentName, ex));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
