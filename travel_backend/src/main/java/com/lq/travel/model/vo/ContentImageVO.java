package com.lq.travel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 页面内容图片视图对象
 */
@Data
public class ContentImageVO implements Serializable {

    private Long id;

    private String alt;

    private String photographer;

    private String photographerUrl;

    private String pexelsUrl;

    private Integer width;

    private Integer height;

    private String mediumUrl;

    private String largeUrl;

    private String large2xUrl;

    private String landscapeUrl;

    private static final long serialVersionUID = 1L;
}
