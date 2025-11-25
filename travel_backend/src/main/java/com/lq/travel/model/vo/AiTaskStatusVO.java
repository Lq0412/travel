package com.lq.travel.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * AI 任务状态返回 VO
 */
@Data
public class AiTaskStatusVO {
    private String taskId;
    private String type;
    private String status;
    private String result;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetry;
    private Date updateTime;
}
