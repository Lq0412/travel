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
     * 照片 URL 列表（3-6 张）
     */
    private List<String> photoUrls;

    /**
     * 模板名称（默认 "default"）
     */
    private String templateName;

    /**
     * 生成尺寸（如 1328x1328），可选
     */
    private String size;

    /**
     * 风格，可选（如 fresh）
     */
    private String style;

    /**
     * 质量，可选（如 standard）
     */
    private String quality;

    private static final long serialVersionUID = 1L;
}
