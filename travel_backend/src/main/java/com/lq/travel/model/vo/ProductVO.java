package com.lq.travel.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProductVO {

    private Long id;

    private String name;

    private String city;

    private String address;

    private List<String> tags;

    private String description;

    private Boolean isRecommendable;

    private Boolean isPurchasable;

    private String cover;

    private Double latitude;

    private Double longitude;

    private Date createTime;

    private Date updateTime;
}
