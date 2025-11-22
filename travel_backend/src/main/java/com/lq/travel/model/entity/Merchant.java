package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private Integer type;
    private String introduction;
    private String coverUrl;
    private String logoUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String location;
    private String openHours;
    private String contactPhone;
    private BigDecimal rating;
    private Integer status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String tags;
    private String businessHours;
    private BigDecimal serviceScore;
    private BigDecimal environmentScore;
    private String featureTags;
    private Integer isVerified;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}