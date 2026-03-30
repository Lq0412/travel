package com.lq.travel.model.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建数据补齐任务请求
 */
@Data
public class KnowledgeIngestionTaskCreateRequest {

    @NotBlank(message = "query 不能为空")
    private String query;

    /**
     * 数据源策略：AUTO / TAVILY / DASHSCOPE
     */
    private String dataSource;

    /**
     * 执行效果：FAST / BALANCED / DEEP
     */
    private String effectPreset;

    @Min(value = 1, message = "maxItems 不能小于 1")
    @Max(value = 30, message = "maxItems 不能大于 30")
    private Integer maxItems;

    private Boolean mustContainStoreName;

    @Min(value = 1, message = "maxRetry 不能小于 1")
    @Max(value = 10, message = "maxRetry 不能大于 10")
    private Integer maxRetry;
}