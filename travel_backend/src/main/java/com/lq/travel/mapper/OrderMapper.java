package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    IPage<Order> selectByUser(Page<Order> page, @Param("userId") Long userId);

    Order selectDetail(@Param("orderId") Long orderId, @Param("userId") Long userId);

    IPage<Order> selectByMerchant(Page<Order> page, @Param("merchantId") Long merchantId);

    Order selectDetailByMerchant(@Param("orderId") Long orderId, @Param("merchantId") Long merchantId);
}


