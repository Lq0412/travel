package com.lq.travel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.PageRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.model.entity.Merchant;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.MerchantService;
import com.lq.travel.service.ProductService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;

@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private UserService userService;

    @Resource
    private MerchantService merchantService;

    // ================= 用户端接口 =================

    // 1. 获取商品详情
    @GetMapping("/get/{id}")
    public ResponseDTO<Product> getProductById(@PathVariable Long id) {
        return ResponseUtils.success(productService.getById(id));
    }

    // 2. 分页获取商品列表（用户端）
    @GetMapping("/list")
    public ResponseDTO<IPage<Product>> listProducts(@RequestParam(required = false) Long merchantId,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam(required = false, defaultValue = "1") Integer current,
                                                      @RequestParam(required = false, defaultValue = "12") Integer pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1); // 只显示上架商品
        
        if (merchantId != null) {
            queryWrapper.eq("merchant_id", merchantId);
        }
        if (category != null && !category.isEmpty()) {
            queryWrapper.like("category", category);
        }
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        queryWrapper.orderByDesc("create_time");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, pageSize);
        return ResponseUtils.success(productService.page(page, queryWrapper));
    }

    // ================= 商家端接口 =================

    @PostMapping("/merchant/add")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<Long> merchantAddProduct(@RequestBody Product product, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        product.setMerchantId(merchant.getId());
        boolean saved = productService.save(product);
        return ResponseUtils.success(saved ? product.getId() : 0L);
    }

    @PostMapping("/merchant/update")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<Boolean> merchantUpdateProduct(@RequestBody Product product, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        // 验证商品是否属于当前商家
        Product existingProduct = productService.getById(product.getId());
        if (existingProduct == null) {
            throw new com.lq.travel.exception.BusinessException(com.lq.travel.exception.ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        if (!existingProduct.getMerchantId().equals(merchant.getId())) {
            throw new com.lq.travel.exception.BusinessException(com.lq.travel.exception.ErrorCode.NO_AUTH_ERROR, "无权限修改此商品");
        }
        
        return ResponseUtils.success(productService.updateById(product));
    }

    @PostMapping("/merchant/delete")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<Boolean> merchantDeleteProduct(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        Product product = productService.getById(deleteRequest.getId());
        if (product == null) {
            throw new com.lq.travel.exception.BusinessException(com.lq.travel.exception.ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        if (!product.getMerchantId().equals(merchant.getId())) {
            throw new com.lq.travel.exception.BusinessException(com.lq.travel.exception.ErrorCode.NO_AUTH_ERROR, "无权限删除此商品");
        }
        
        return ResponseUtils.success(productService.removeById(deleteRequest.getId()));
    }

    @GetMapping("/merchant/list")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<IPage<Product>> merchantListProducts(
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("merchant_id", merchant.getId());
        queryWrapper.eq("is_delete", 0);
        
        // 添加筛选条件
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name.trim());
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, pageSize);
        return ResponseUtils.success(productService.page(page, queryWrapper));
    }

    // ================= 管理端 CRUD =================

    @PostMapping("/admin/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Long> adminAddProduct(@RequestBody Product product, HttpServletRequest request) {
        boolean saved = productService.save(product);
        return ResponseUtils.success(saved ? product.getId() : 0L);
    }

    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> adminUpdateProduct(@RequestBody Product product) {
        return ResponseUtils.success(productService.updateById(product));
    }

    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> adminDeleteProduct(@RequestBody DeleteRequest deleteRequest) {
        return ResponseUtils.success(productService.removeById(deleteRequest.getId()));
    }

    @GetMapping("/admin/get/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Product> adminGetProduct(@PathVariable Long id) {
        return ResponseUtils.success(productService.getById(id));
    }

    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<IPage<Product>> adminListProducts(@RequestBody(required = false) ProductQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new ProductQueryRequest();
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return ResponseUtils.success(productService.page(page));
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ProductQueryRequest extends PageRequest implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}

