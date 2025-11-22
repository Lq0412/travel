package com.lq.travel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lq.travel.model.dto.order.CreateOrderRequest;
import com.lq.travel.model.entity.Order;
import com.lq.travel.model.entity.OrderItem;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface OrderService {
    Long createOrder(CreateOrderRequest request, HttpServletRequest httpRequest);

    IPage<Order> listMyOrders(long current, long size, HttpServletRequest httpRequest);

    Order getMyOrderDetail(Long orderId, HttpServletRequest httpRequest);

    List<OrderItem> listOrderItems(Long orderId);

    IPage<Order> listMerchantOrders(long current, long size, String orderNo, Integer status, HttpServletRequest httpRequest);

    Order getMerchantOrderDetail(Long orderId, HttpServletRequest httpRequest);

    Boolean payOrder(Long orderId, Integer payMethod, HttpServletRequest httpRequest);

    Boolean cancelOrder(Long orderId, String reason, HttpServletRequest httpRequest);

    Boolean shipOrder(Long orderId, String trackingNumber, HttpServletRequest httpRequest);

    Boolean merchantCancelOrder(Long orderId, String reason, HttpServletRequest httpRequest);
}


