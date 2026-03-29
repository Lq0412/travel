package com.lq.travel.AI.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 景点知识库实体
 */
@Data
@TableName("tb_knowledge_attraction")
public class KnowledgeAttraction implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 景点名称
     */
    private String name;

    /**
     * 景点描述
     */
    private String description;

    /**
     * 类型：自然/文化/温泉/美食/休闲
     */
    private String category;

    /**
     * 最佳季节：春/夏/秋/冬/全年
     */
    private String season;

    /**
     * 适合人群：家庭/情侣/老人/儿童/年轻人
     */
    private String suitableFor;

    /**
     * 价格等级：1-5（1=经济，5=豪华）
     */
    private Integer priceLevel;

    /**
     * 评分（0-5.0）
     */
    private Double rating;

    /**
     * 标签（JSON格式）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 地址
     */
    private String address;

    /**
     * 营业时间
     */
    private String openingHours;

    /**
     * 门票价格
     */
    private String ticketPrice;

    /**
     * 特色亮点（JSON格式）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> features;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}

