package com.lq.travel.AI.core.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理请求模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentRequest {
    
    /**
     * 任务描述
     */
    @NotBlank(message = "任务描述不能为空")
    private String task;
    
    /**
     * 上下文信息
     */
    private String context;

    /**
     * 多轮对话历史
     */
    private List<AIRequest.Message> messages;
    /**
     * 目标
     */
    private String goal;
    
    /**
     * 约束条件
     */
    private String constraints;
    
    /**
     * 最大执行步数
     */
    @Min(value = 1, message = "最大执行步数不能小于1")
    @Max(value = 50, message = "最大执行步数不能大于50")
    @Builder.Default
    private Integer maxSteps = 10;
    
    /**
     * 是否启用工具调用
     */
    @Builder.Default
    private Boolean enableTools = true;
    
    /**
     * 扩展参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
}
