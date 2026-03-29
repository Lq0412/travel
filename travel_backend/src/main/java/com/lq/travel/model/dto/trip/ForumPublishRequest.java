package com.lq.travel.model.dto.trip;

import lombok.Data;

import java.io.Serializable;

/**
 * 一键发布论坛请求
 */
@Data
public class ForumPublishRequest implements Serializable {
    
    /**
     * 行程ID
     */
    private Long tripId;
    
    /**
     * 标题（可选，不传则自动生成）
     */
    private String title;
    
    /**
     * 正文（可选，不传则自动生成）
     */
    private String content;
    
    /**
     * 分类ID（可选，不传则使用默认分类）
     */
    private Integer categoryId;
    
    /**
     * 标签列表（可选，不传则自动生成）
     */
    private java.util.List<String> tags;
    
    private static final long serialVersionUID = 1L;
}

