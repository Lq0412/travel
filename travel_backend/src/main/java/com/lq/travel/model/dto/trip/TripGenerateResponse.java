package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 行程生成响应
 */
@Data
public class TripGenerateResponse implements Serializable {
    
    /**
     * 生成的方案列表（1-2个）
     */
    private List<TripPlan> plans;
    
    /**
     * 行程方案
     */
    @Data
    public static class TripPlan implements Serializable {
        /**
         * 方案ID（临时ID，用于前端选择）
         */
        private String planId;
        
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
         * 主题
         */
        private String theme;
        
        /**
         * 按天亮点（Map<day, List<highlights>>）
         */
        private Map<Integer, List<String>> dailyHighlights;
        
        /**
         * 方案描述
         */
        private String description;
        
        private static final long serialVersionUID = 1L;
    }
    
    private static final long serialVersionUID = 1L;
}

