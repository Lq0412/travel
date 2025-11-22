package com.lq.travel.common;

import lombok.Data;


/**
 * 通用的分类请求页
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private long current = 1;
    /**
     * 页面大小
     */
    private long pageSize = 10;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = "descend";
}
