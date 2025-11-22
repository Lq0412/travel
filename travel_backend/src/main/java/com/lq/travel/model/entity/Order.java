package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private BigDecimal actualAmount;
    private BigDecimal discountAmount;
    private Integer status;
    private LocalDateTime payTime;
    private Integer payMethod;
    private String transactionId;
    private String contactName;
    private String contactPhone;
    private String remark;
    private String shippingAddress;
    private String invoiceInfo;
    private String cancelReason;
    private String refundReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}


