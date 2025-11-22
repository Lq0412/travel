package com.lq.travel.AI.core.agent.impl;

import com.lq.travel.AI.core.agent.BaseAgent;
import com.lq.travel.AI.core.constants.AIModelConfig;
import com.lq.travel.AI.core.model.AgentRequest;
import com.lq.travel.AI.core.model.AgentResponse;
import com.lq.travel.AI.core.model.AIRequest;
import com.lq.travel.AI.core.model.AIResponse;
import com.lq.travel.AI.core.service.AIService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ReAct代理实现
 * 基于Reasoning和Acting的代理模式
 */
@Slf4j
public class ReActAgent extends BaseAgent {
    
    public ReActAgent(String name, AIService aiService) {
        super(name, "ReAct代理 - 基于推理和行动的智能代理", aiService);
    }
    
    @Override
    protected AgentResponse.Step executeStep(int stepNumber, AgentRequest request, List<AgentResponse.Step> previousSteps) {
        try {
            // 构建当前步骤的提示词
            String prompt = buildStepPrompt(stepNumber, request, previousSteps);
            
            // 调用AI服务
            AIRequest aiRequest = AIRequest.builder()
                    .message(prompt)
                    .systemPrompt(getSystemPrompt())
                    .temperature(AIModelConfig.TEMPERATURE_REACT)
                    .maxTokens(AIModelConfig.MAX_TOKENS_REACT)
                    .build();
            
            AIResponse aiResponse = aiService.chat(aiRequest);
            
            if (!aiResponse.getSuccess()) {
                return AgentResponse.Step.builder()
                        .stepNumber(stepNumber)
                        .action("error")
                        .observation("AI调用失败: " + aiResponse.getErrorMessage())
                        .reasoning("无法获取AI响应")
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
            
            // 解析AI响应
            String response = aiResponse.getContent();
            String[] parts = parseReActResponse(response);
            
            return AgentResponse.Step.builder()
                    .stepNumber(stepNumber)
                    .action(parts[0]) // 行动
                    .observation(parts[1]) // 观察
                    .reasoning(parts[2]) // 推理
                    .timestamp(System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            log.error("执行步骤失败", e);
            return AgentResponse.Step.builder()
                    .stepNumber(stepNumber)
                    .action("error")
                    .observation("步骤执行异常: " + e.getMessage())
                    .reasoning("系统错误")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
    
    @Override
    protected boolean isTaskCompleted(AgentResponse.Step currentStep, AgentRequest request) {
        // 检查是否包含完成标识
        String observation = currentStep.getObservation().toLowerCase();
        return observation.contains("任务完成") || 
               observation.contains("task completed") ||
               observation.contains("最终答案") ||
               observation.contains("final answer");
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            你是一个ReAct代理，需要按照以下格式进行推理和行动：
            
            思考: [你的推理过程]
            行动: [你要采取的具体行动]
            观察: [行动的结果或观察到的信息]
            
            请根据用户的任务描述，逐步推理并采取行动来完成任务。
            当你找到最终答案时，请在观察中明确说明"任务完成"或"最终答案"。
            """;
    }
    
    @Override
    public String[] getCapabilities() {
        return new String[]{"推理", "行动", "观察", "问题解决"};
    }
    
    /**
     * 构建步骤提示词
     */
    private String buildStepPrompt(int stepNumber, AgentRequest request, List<AgentResponse.Step> previousSteps) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("任务: ").append(request.getTask()).append("\n");
        if (request.getContext() != null) {
            prompt.append("上下文: ").append(request.getContext()).append("\n");
        }
        if (request.getGoal() != null) {
            prompt.append("目标: ").append(request.getGoal()).append("\n");
        }
        if (request.getConstraints() != null) {
            prompt.append("约束: ").append(request.getConstraints()).append("\n");
        }
        
        prompt.append("\n当前是第").append(stepNumber).append("步。\n");
        
        if (!previousSteps.isEmpty()) {
            prompt.append("\n之前的步骤:\n");
            for (AgentResponse.Step step : previousSteps) {
                prompt.append("步骤").append(step.getStepNumber()).append(":\n");
                prompt.append("  行动: ").append(step.getAction()).append("\n");
                prompt.append("  观察: ").append(step.getObservation()).append("\n");
                prompt.append("  推理: ").append(step.getReasoning()).append("\n\n");
            }
        }
        
        prompt.append("请继续执行下一步:");
        
        return prompt.toString();
    }
    
    /**
     * 解析ReAct响应
     */
    private String[] parseReActResponse(String response) {
        String[] parts = new String[3];
        parts[0] = "思考"; // 默认行动
        parts[1] = response; // 默认观察
        parts[2] = "基于AI响应进行推理"; // 默认推理
        
        try {
            // 简单的解析逻辑，可以根据需要优化
            if (response.contains("思考:")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.startsWith("思考:")) {
                        parts[2] = line.substring(3).trim();
                    } else if (line.startsWith("行动:")) {
                        parts[0] = line.substring(3).trim();
                    } else if (line.startsWith("观察:")) {
                        parts[1] = line.substring(3).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析ReAct响应失败，使用默认值", e);
        }
        
        return parts;
    }
}
