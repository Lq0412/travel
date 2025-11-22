package com.lq.travel.AI.core.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 美食知识库实体
 */
@Data
@TableName("tb_knowledge_food")
public class KnowledgeFood implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 美食名称
     */
    private String name;

    /**
     * 美食描述
     */
    private String description;

    /**
     * 类型：主食/小吃/特产/饮品
     */
    private String category;

    /**
     * 口味：甜/咸/辣/酸/鲜
     */
    private String taste;

    /**
     * 价格区间
     */
    private String priceRange;

    /**
     * 推荐餐厅
     */
    private String whereToEat;

    /**
     * 最佳季节
     */
    private String season;

    /**
     * 标签（JSON格式）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}

