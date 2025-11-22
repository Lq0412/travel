package com.lq.travel.AI.core.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * AI请求模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIRequest {
    /**
     * 指定使用的AI提供商
     */
    private String provider;
    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 使用的模型名称
     */
    private String model;
    
    /**
     * 温度参数 (0.0-2.0)
     */
    @DecimalMin(value = "0.0", message = "温度参数不能小于0.0")
    @DecimalMax(value = "2.0", message = "温度参数不能大于2.0")
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * 最大token数
     */
    @Min(value = 1, message = "最大token数不能小于1")
    @Max(value = 10000, message = "最大token数不能大于10000")
    @Builder.Default
    private Integer maxTokens = 1000;
    
    /**
     * 是否流式响应
     */
    @Builder.Default
    private Boolean stream = false;
    
    /**
     * 对话历史
     */
    @Builder.Default
    private List<Message> history = new ArrayList<>();
    
    /**
     * 扩展参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role; // user, assistant, system
        private String content;
        private Long timestamp;
    }
}
