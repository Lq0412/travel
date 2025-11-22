package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言墙主表
 */
@Data
@TableName("message_wall")
public class MessageWall {
    @TableId(type = IdType.AUTO)
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
    private Boolean isBarrage;
    private Integer barrageSpeed;
    private Integer barrageTrajectory;
    private Boolean isAnonymous;
    private Integer messageType;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}