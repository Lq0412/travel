package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.MerchantMapper;
import com.lq.travel.mapper.ProductMapper;
import com.lq.travel.model.dto.merchant.MerchantQueryRequest;
import com.lq.travel.model.entity.Merchant;
import com.lq.travel.model.entity.Product;
import com.lq.travel.service.MerchantService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public IPage<Merchant> getMerchantList(MerchantQueryRequest queryRequest) {
        Merchant merchant = new Merchant();
        BeanUtils.copyProperties(queryRequest, merchant);
        
        QueryWrapper<Merchant> queryWrapper = new QueryWrapper<>(merchant);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        
        // 按评分和创建时间排序
        queryWrapper.orderByDesc("rating", "create_time");
        
        Page<Merchant> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return this.page(page, queryWrapper);
    }

    @Override
    public Merchant getMerchantDetail(Long merchantId) {
        Merchant merchant = this.getById(merchantId);
        if (merchant == null || merchant.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商家不存在");
        }
        if (merchant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "商家已下架");
        }
        return merchant;
    }

    @Override
    public IPage<Product> getMerchantProducts(MerchantQueryRequest queryRequest, Long merchantId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("merchant_id", merchantId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        
        // 按创建时间排序
        queryWrapper.orderByDesc("create_time");
        
        Page<Product> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return productMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<Merchant> getRecommendedMerchants(Integer limit) {
        QueryWrapper<Merchant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("rating", "create_time");
        queryWrapper.last("LIMIT " + limit);
        
        return this.list(queryWrapper);
    }

    @Override
    public IPage<Merchant> searchMerchants(MerchantQueryRequest queryRequest) {
        QueryWrapper<Merchant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        
        // 名称搜索
        if (StringUtils.isNotBlank(queryRequest.getName())) {
            queryWrapper.like("name", queryRequest.getName());
        }
        
        // 类型搜索
        if (queryRequest.getType() != null) {
            queryWrapper.eq("type", queryRequest.getType());
        }
        
        // 搜索关键词（同时搜索名称、介绍等）
        if (StringUtils.isNotBlank(queryRequest.getSearchText())) {
            queryWrapper.and(wrapper -> wrapper
                .like("name", queryRequest.getSearchText())
                .or()
                .like("introduction", queryRequest.getSearchText())
            );
        }
        
        // 评分范围搜索
        if (queryRequest.getMinRating() != null) {
            queryWrapper.ge("rating", queryRequest.getMinRating());
        }
        if (queryRequest.getMaxRating() != null) {
            queryWrapper.le("rating", queryRequest.getMaxRating());
        }
        
        // 位置搜索
        if (StringUtils.isNotBlank(queryRequest.getLocation())) {
            queryWrapper.like("location", queryRequest.getLocation());
        }
        
        // 按评分和创建时间排序
        queryWrapper.orderByDesc("rating", "create_time");
        
        Page<Merchant> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return this.page(page, queryWrapper);
    }

    @Override
    public IPage<Merchant> adminListMerchants(MerchantQueryRequest queryRequest) {
        QueryWrapper<Merchant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);

        // 管理端可按名称、类型、状态筛选
        if (StringUtils.isNotBlank(queryRequest.getName())) {
            queryWrapper.like("name", queryRequest.getName());
        }
        if (queryRequest.getType() != null) {
            queryWrapper.eq("type", queryRequest.getType());
        }
        if (queryRequest.getStatus() != null) {
            queryWrapper.eq("status", queryRequest.getStatus());
        }

        queryWrapper.orderByDesc("create_time");

        Page<Merchant> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return this.page(page, queryWrapper);
    }

    @Override
    public Merchant getCurrentMerchantByUserId(Long userId) {
        Merchant merchant = this.getOne(
            new QueryWrapper<Merchant>()
                .eq("user_id", userId)
                .eq("is_delete", 0)
        );
        
        if (merchant == null) {
            throw new BusinessException(
                ErrorCode.NO_AUTH_ERROR, 
                "您还不是商家，请联系管理员创建商家信息。您的用户ID: " + userId
            );
        }
        
        return merchant;
    }
}
