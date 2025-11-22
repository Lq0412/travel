package com.lq.travel.model.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest implements Serializable {
    private Long merchantId;
    private String contactName;
    private String contactPhone;
    private String shippingAddress;
    private String remark;
    private Integer payMethod; // 1-微信,2-支付宝

    private List<CreateOrderItem> items;

    @Data
    public static class CreateOrderItem implements Serializable {
        private Long productId;
        private Integer quantity;
        private BigDecimal price; // optional client hint; server will verify
        private String specs;
    }
}


