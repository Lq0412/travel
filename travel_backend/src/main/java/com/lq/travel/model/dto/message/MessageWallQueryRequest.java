package com.lq.travel.model.dto.message;

import lombok.Data;

@Data
public class MessageWallQueryRequest {
    private Long scenicSpotId;
    private Long userId;
    private Integer messageType;
    private Integer status;
    private Boolean isBarrage;
    private String keyword;
    private Integer pageSize = 10;
    private Integer current = 1;
}