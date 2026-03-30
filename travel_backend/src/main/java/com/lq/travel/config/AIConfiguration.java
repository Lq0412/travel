package com.lq.travel.config;

import com.lq.travel.agent.impl.GenericTravelAgent;
import com.lq.travel.service.AIService;
import com.lq.travel.service.AgentService;
import com.lq.travel.service.TravelRagService;
import com.lq.travel.service.impl.MilvusKnowledgeSyncService;
import com.lq.travel.service.impl.TravelMultiAgentCoordinator;
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

    @Autowired
    private TravelRagService travelRagService;

    @Autowired
    private TravelMultiAgentCoordinator travelMultiAgentCoordinator;

    @Autowired
    private MilvusKnowledgeSyncService milvusKnowledgeSyncService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化AI模块...");
        
        // 注册默认代理
        registerDefaultAgents();

        // 按配置执行 Milvus 初始化/同步
        milvusKnowledgeSyncService.autoSyncOnStartupIfEnabled();
        
        log.info("AI模块初始化完成");
        log.info("可用的AI提供商: {}", String.join(", ", aiService.getAvailableProviders()));
        log.info("可用的代理: {}", String.join(", ", agentService.getAvailableAgents()));
    }
    
    /**
     * 注册默认代理
     */
    private void registerDefaultAgents() {
        // 注册通用旅行代理
        GenericTravelAgent genericTravelAgent = new GenericTravelAgent(
                "generic-travel",
                aiService,
                travelRagService,
                travelMultiAgentCoordinator
        );
        agentService.registerAgent("generic-travel", genericTravelAgent);
        log.debug("通用旅行代理已注册: generic-travel");
    }
}
