package com.lq.travel.AI.core.config;

import com.lq.travel.AI.core.agent.impl.ReActAgent;
import com.lq.travel.AI.core.agent.impl.CongHuaTourismAgent;
import com.lq.travel.AI.core.service.AIService;
import com.lq.travel.AI.core.service.AgentService;
import com.lq.travel.AI.core.service.KnowledgeService;
import com.lq.travel.AI.core.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类
 * 负责初始化AI服务和代理
 */
@Slf4j
@Configuration
public class AIConfiguration implements CommandLineRunner {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private AgentService agentService;
    
    @Autowired(required = false)
    private KnowledgeService knowledgeService;
    
    @Autowired(required = false)
    private AIMessageService messageService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化AI模块...");
        
        // 注册默认代理
        registerDefaultAgents();
        
        log.info("AI模块初始化完成");
        log.info("可用的AI提供商: {}", String.join(", ", aiService.getAvailableProviders()));
        log.info("可用的代理: {}", String.join(", ", agentService.getAvailableAgents()));
    }
    
    /**
     * 注册默认代理
     */
    private void registerDefaultAgents() {
        // 注册ReAct代理
        ReActAgent reactAgent = new ReActAgent("react", aiService);
        agentService.registerAgent("react", reactAgent);
        
        // 注册从化旅游代理
        CongHuaTourismAgent tourismAgent = new CongHuaTourismAgent("conghua-tourism", aiService);
        
        // 注入知识库服务（如果可用）
        if (knowledgeService != null) {
            tourismAgent.setKnowledgeService(knowledgeService);
            log.info("✅ 知识库服务已注入到从化旅游代理");
        } else {
            log.warn("⚠️ 知识库服务未启用，将使用默认知识库");
        }
        
        // 注入消息服务（如果可用）
        if (messageService != null) {
            tourismAgent.setMessageService(messageService);
            log.info("✅ 消息服务已注入到从化旅游代理");
        }
        
        agentService.registerAgent("conghua-tourism", tourismAgent);
        
        // 可以在这里注册更多代理
        // ToolCallAgent toolAgent = new ToolCallAgent("tool", aiService);
        // agentService.registerAgent("tool", toolAgent);
    }
}
