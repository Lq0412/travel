package com.lq.travel.model.dto.product;

import lombok.Data;

import java.util.List;

@Data
public class ProductSaveRequest {

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
}
