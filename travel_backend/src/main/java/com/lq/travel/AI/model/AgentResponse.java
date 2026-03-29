package com.lq.travel.AI.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 代理响应模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentResponse {
    
    /**
     * 最终结果
     */
    private String result;
    
    /**
     * 执行步骤
     */
    @Builder.Default
    private List<Step> steps = new ArrayList<>();
    
    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = true;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行时间(毫秒)
     */
    private Long executionTime;
    
    /**
     * 使用的工具
     */
    @Builder.Default
    private List<String> toolsUsed = new ArrayList<>();
    
    /**
     * 扩展信息
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Step {
        private Integer stepNumber;
        private String action;
        private String observation;
        private String reasoning;
        private Long timestamp;
    }
    
    /**
     * 创建成功响应
     */
    public static AgentResponse success(String result) {
        return AgentResponse.builder()
                .result(result)
                .success(true)
                .build();
    }
    
    /**
     * 创建带步骤的成功响应
     */
    public static AgentResponse successWithSteps(String result, List<Step> steps) {
        return AgentResponse.builder()
                .result(result)
                .steps(steps)
                .success(true)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static AgentResponse error(String errorMessage) {
        return AgentResponse.builder()
                .errorMessage(errorMessage)
                .success(false)
                .build();
    }
}
