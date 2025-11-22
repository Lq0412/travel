package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;

/**
 * 行程生成请求
 */
@Data
public class TripGenerateRequest implements Serializable {
    
    /**
     * 目的地
     */
    private String destination;
    
    /**
     * 天数
     */
    private Integer days;
    
    /**
     * 预算（元）
     */
    private Integer budget;
    
    /**
     * 主题（如：休闲、探险、文化等）
     */
    private String theme;
    
    private static final long serialVersionUID = 1L;
}

