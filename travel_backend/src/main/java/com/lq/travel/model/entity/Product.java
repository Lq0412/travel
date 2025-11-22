package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;
    private String name;
    private Long categoryId;
    private String category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String coverUrl;
    private String images;
    private String description;
    private String specs;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private Integer isRecommend;
    private BigDecimal weight;
    private String serviceGuarantee;
    private Integer limitPerUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}