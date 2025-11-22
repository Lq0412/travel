package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点信息表
 */
@Data
@TableName("scenic_spot")
public class ScenicSpot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String introduction;
    private String coverUrl;
    private String messageWallTitle;
    private String messageWallDescription;
    private String location;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String tags;
    private String openHours;
    private String ticketInfo;
    private BigDecimal rating;
    private Integer heatValue;
    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}

