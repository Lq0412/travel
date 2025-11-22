package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 回忆图生成请求
 */
@Data
public class MemoryCardGenerateRequest implements Serializable {
    
    /**
     * 行程ID
     */
    private Long tripId;
    
    /**
     * 照片URL列表（3-6张）
     */
    private List<String> photoUrls;
    
    /**
     * 模板名称（MVP阶段固定为"default"）
     */
    private String templateName;
    
    private static final long serialVersionUID = 1L;
}

