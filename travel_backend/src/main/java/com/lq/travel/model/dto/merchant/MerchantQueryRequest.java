package com.lq.travel.model.dto.merchant;

import com.lq.travel.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 商家查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MerchantQueryRequest extends PageRequest implements Serializable {

    /**
     * 商家ID
     */
    private Long id;

    /**
     * 商家名称
     */
    private String name;

    /**
     * 商家类型
     */
    private Integer type;

    /**
     * 商家状态：0-待审核 1-正常 2-下架
     */
    private Integer status;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 搜索关键词（同时搜索名称、介绍等）
     */
    private String searchText;

    /**
     * 最低评分
     */
    private Double minRating;

    /**
     * 最高评分
     */
    private Double maxRating;

    /**
     * 位置（用于附近商家搜索）
     */
    private String location;

    private static final long serialVersionUID = 1L;
}
