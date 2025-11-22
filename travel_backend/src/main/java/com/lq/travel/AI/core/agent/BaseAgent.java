package com.lq.travel.AI.core.agent;

import com.lq.travel.AI.core.interfaces.Agent;
import com.lq.travel.AI.core.model.AgentRequest;
import com.lq.travel.AI.core.model.AgentResponse;
import com.lq.travel.AI.core.service.AIService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础代理抽象类
 * 提供通用的代理功能实现
 */
@Data
@Slf4j
public abstract class BaseAgent implements Agent {
    
    protected String name;
    protected String description;
    protected AIService aiService;
    protected int maxSteps = 10;
    protected boolean available = true;
    
    public BaseAgent(String name, String description, AIService aiService) {
        this.name = name;
        this.description = description;
        this.aiService = aiService;
    }
    
    @Override
    public AgentResponse execute(AgentRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            
            log.info("开始执行代理任务: {}", name);
            
            List<AgentResponse.Step> steps = new ArrayList<>();
            String result = "";
            
            // 执行代理逻辑
            for (int i = 0; i < request.getMaxSteps(); i++) {
                AgentResponse.Step step = executeStep(i + 1, request, steps);
                steps.add(step);
                
                // 检查是否完成
                if (isTaskCompleted(step, request)) {
                    result = step.getObservation();
                    break;
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return AgentResponse.builder()
                    .result(result)
                    .steps(steps)
                    .executionTime(executionTime)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("代理执行失败: {}", name, e);
            return AgentResponse.error("代理执行失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isAvailable() {
        return available && aiService != null;
    }
    
    @Override
    public String[] getCapabilities() {
        // 默认返回空数组，子类可以覆盖此方法
        return new String[0];
    }
    
    /**
     * 执行单个步骤
     */
    protected abstract AgentResponse.Step executeStep(int stepNumber, AgentRequest request, List<AgentResponse.Step> previousSteps);
    
    /**
     * 判断任务是否完成
     */
    protected abstract boolean isTaskCompleted(AgentResponse.Step currentStep, AgentRequest request);
    
    /**
     * 获取系统提示词
     */
    protected abstract String getSystemPrompt();
}
