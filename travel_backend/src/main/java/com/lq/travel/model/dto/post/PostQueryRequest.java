package com.lq.travel.model.dto.post;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostQueryRequest implements Serializable {
    private Integer current = 1;      // 当前页码
    private Integer pageSize = 10;    // 每页数量
    private String sortField;         // 排序字段
    private String keyword;           // 搜索关键词
    private String tagName;           // 单个标签名称（用于单个标签搜索）
    private List<String> tagNames;   // 多个标签名称（用于多标签搜索）
    private Long tagId;              // 单个标签ID（用于单个标签ID搜索）
    private List<Long> tagIds;       // 多个标签ID（用于多标签ID搜索）
    private Integer categoryId;       // 分类ID
}