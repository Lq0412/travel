package com.lq.travel.model.vo;

import com.lq.travel.model.entity.Product;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartVO implements Serializable {
    private Long id;
    private Long userId;
    private Long productId;
    private Long merchantId;
    private Integer quantity;
    private String specs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Product product;
    private BigDecimal subtotal;

    private static final long serialVersionUID = 1L;
}

