package com.lq.travel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.merchant.MerchantQueryRequest;
import com.lq.travel.model.entity.Merchant;
import com.lq.travel.model.entity.Product;

import java.util.List;

public interface MerchantService extends IService<Merchant> {

    /**
     * 获取商家列表（分页）
     */
    IPage<Merchant> getMerchantList(MerchantQueryRequest queryRequest);

    /**
     * 获取商家详情
     */
    Merchant getMerchantDetail(Long merchantId);

    /**
     * 获取商家商品列表
     */
    IPage<Product> getMerchantProducts(MerchantQueryRequest queryRequest, Long merchantId);

    /**
     * 获取推荐商家
     */
    List<Merchant> getRecommendedMerchants(Integer limit);

    /**
     * 搜索商家
     */
    IPage<Merchant> searchMerchants(MerchantQueryRequest queryRequest);

    /**
     * 管理端分页查询商家（不限制状态）
     */
    IPage<Merchant> adminListMerchants(MerchantQueryRequest queryRequest);

    /**
     * 根据用户ID获取当前登录商家的商家信息
     * @param userId 用户ID
     * @return 商家信息
     * @throws BusinessException 如果不是商家或商家不存在
     */
    Merchant getCurrentMerchantByUserId(Long userId);
}