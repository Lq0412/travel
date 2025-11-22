package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;

/**
 * 回忆图生成响应
 */
@Data
public class MemoryCardGenerateResponse implements Serializable {
    
    /**
     * 任务ID（用于轮询状态）
     */
    private String taskId;
    
    /**
     * 生成状态：pending、processing、success、failed
     */
    private String status;
    
    /**
     * 生成的图片URL（成功时返回）
     */
    private String imageUrl;
    
    /**
     * 错误信息（失败时返回）
     */
    private String errorMessage;
    
    private static final long serialVersionUID = 1L;
}

