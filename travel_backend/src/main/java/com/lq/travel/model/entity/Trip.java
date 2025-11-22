package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行程实体
 */
@Data
@TableName("trip")
public class Trip implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
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
    
    /**
     * 开始日期
     */
    private Date startDate;
    
    /**
     * 结束日期
     */
    private Date endDate;
    
    /**
     * 按天亮点（JSON格式，存储每天的行程安排）
     * 格式：[{"day": 1, "highlights": ["景点1", "景点2"]}, ...]
     */
    private String dailyHighlights;
    
    /**
     * 完整结构化数据（JSON格式，存储AI生成的完整行程数据）
     * 存储整个 StructuredItinerary 对象的 JSON 序列化结果
     */
    private String structuredData;
    
    /**
     * 状态：draft（草稿）、accepted（已接受）、completed（已完成）
     */
    private String status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    
    private static final long serialVersionUID = 1L;
}

