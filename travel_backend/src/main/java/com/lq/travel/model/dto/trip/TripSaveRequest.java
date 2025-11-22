package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.List;

/**
 * 保存行程请求
 */
@Data
public class TripSaveRequest implements Serializable {
    
    /**
     * 方案ID（从生成响应中获取）
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
     * 开始日期
     */
    private Date startDate;
    
    /**
     * 结束日期
     */
    private Date endDate;
    
    /**
     * 按天亮点（Map<day, List<highlights>>）
     */
    private Map<Integer, List<String>> dailyHighlights;
    
    /**
     * 完整结构化数据（JSON字符串）
     * 存储AI生成的完整行程数据
     */
    private String structuredData;
    
    private static final long serialVersionUID = 1L;
}

