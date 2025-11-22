package com.lq.travel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.OrderItemMapper;
import com.lq.travel.mapper.OrderMapper;
import com.lq.travel.mapper.ProductMapper;
import com.lq.travel.model.dto.order.CreateOrderRequest;
import com.lq.travel.model.entity.Merchant;
import com.lq.travel.model.entity.Order;
import com.lq.travel.model.entity.OrderItem;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.MerchantService;
import com.lq.travel.service.OrderService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private UserService userService;
    @Resource
    private MerchantService merchantService;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单参数无效");
        }

        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 计算金额并校验库存与状态
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderRequest.CreateOrderItem itemReq : request.getItems()) {
            Product product = productMapper.selectById(itemReq.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "商品不可购买");
            }
            if (product.getStock() != null && product.getStock() < itemReq.getQuantity()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存不足");
            }
            BigDecimal price = product.getPrice();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getCoverUrl());
            orderItem.setPrice(price);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setTotalPrice(lineTotal);
            orderItem.setSpecs(itemReq.getSpecs());
            orderItem.setRefundStatus(0);
            items.add(orderItem);
        }

        // 构建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(loginUser.getId());
        order.setMerchantId(request.getMerchantId());
        order.setTotalAmount(totalAmount);
        order.setActualAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setStatus(0); // 待支付
        order.setPayMethod(request.getPayMethod());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setRemark(request.getRemark());
        this.save(order);

        // 保存订单明细
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        return order.getId();
    }

    @Override
    public IPage<Order> listMyOrders(long current, long size, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<Order> page = new Page<>(current, size);
        return this.baseMapper.selectByUser(page, loginUser.getId());
    }

    @Override
    public Order getMyOrderDetail(Long orderId, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return this.baseMapper.selectDetail(orderId, loginUser.getId());
    }

    @Override
    public List<OrderItem> listOrderItems(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }

    @Override
    public IPage<Order> listMerchantOrders(long current, long size, String orderNo, Integer status, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        Page<Order> page = new Page<>(current, size);
        
        // 使用QueryWrapper进行筛选
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("merchant_id", merchant.getId());
        queryWrapper.eq("is_delete", 0);
        
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            queryWrapper.like("order_no", orderNo.trim());
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public Order getMerchantOrderDetail(Long orderId, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        return this.baseMapper.selectDetailByMerchant(orderId, merchant.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId, Integer payMethod, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        if (!order.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单状态不正确");
        }

        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        order.setTransactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));

        // 扣减库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null && product.getStock() != null) {
                int newStock = product.getStock() - item.getQuantity();
                if (newStock < 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存不足");
                }
                product.setStock(newStock);
                productMapper.updateById(product);
            }
        }

        return this.updateById(order);
    }

    @Override
    public Boolean cancelOrder(Long orderId, String reason, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        if (!order.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此订单");
        }

        // 只能取消待支付订单
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能取消待支付订单");
        }

        // 更新订单状态
        order.setStatus(2); // 已取消
        order.setCancelReason(reason);

        return this.updateById(order);
    }

    @Override
    @Transactional
    public Boolean shipOrder(Long orderId, String trackingNumber, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        // 验证订单是否属于当前商家
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        if (!order.getMerchantId().equals(merchant.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此订单");
        }

        // 只能对已支付的订单发货
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能对已支付订单进行发货");
        }

        // 更新订单状态为已完成（已发货）
        // 注意：当前订单状态设计中，status=3表示已完成
        // 如果需要区分"已发货"和"已完成"，可以新增状态值，这里暂时使用3
        order.setStatus(3); // 已完成（已发货）
        
        // 如果有物流单号，可以存储在remark或其他字段中
        // 如果Order实体有trackingNumber字段，可以使用
        if (StrUtil.isNotBlank(trackingNumber)) {
            if (StrUtil.isBlank(order.getRemark())) {
                order.setRemark("物流单号：" + trackingNumber);
            } else {
                order.setRemark(order.getRemark() + " | 物流单号：" + trackingNumber);
            }
        }

        return this.updateById(order);
    }

    @Override
    @Transactional
    public Boolean merchantCancelOrder(Long orderId, String reason, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        // 验证订单是否属于当前商家
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        if (!order.getMerchantId().equals(merchant.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此订单");
        }

        // 商家可以取消待支付或已支付的订单
        Integer originalStatus = order.getStatus();
        if (originalStatus == null || (originalStatus != 0 && originalStatus != 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能取消待支付或已支付订单");
        }

        // 如果是已支付的订单，需要退还库存
        if (originalStatus == 1) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null && product.getStock() != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                }
            }
        }

        // 更新订单状态
        order.setStatus(2); // 已取消
        order.setCancelReason(reason);

        return this.updateById(order);
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}


