package com.lq.travel.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageWallVO {
    private Long id;
    private Long scenicSpotId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Integer likes;
    private String textColor;
    private Integer fontSize;
    private String backgroundColor;
    private Long backgroundId;
    private String backgroundUrl;
    private Boolean isBarrage;
    private Integer barrageSpeed;
    private Integer barrageTrajectory;
    private Boolean isAnonymous;
    private Integer messageType;
    private Integer status;
    private Boolean isLiked;
    private LocalDateTime createTime;
}