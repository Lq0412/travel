package com.lq.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.entity.Cart;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.CartVO;
import com.lq.travel.service.CartService;
import com.lq.travel.service.ProductService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @Resource
    private UserService userService;

    @Resource
    private ProductService productService;

    @PostMapping("/add")
    public ResponseDTO<Boolean> addToCart(@RequestBody Cart cart, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "请先登录");
        }

        // 校验商品
        Product product = productService.getById(cart.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() == 0) {
            return ResponseUtils.error(400, "商品不存在或已下架");
        }

        // 校验库存
        if (product.getStock() != null && product.getStock() < cart.getQuantity()) {
            return ResponseUtils.error(400, "库存不足");
        }

        // 设置用户ID
        cart.setUserId(loginUser.getId());

        // 查询是否已存在相同商品（相同规格）
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("product_id", cart.getProductId());
        queryWrapper.eq("is_delete", 0);
        if (cart.getSpecs() != null) {
            queryWrapper.eq("specs", cart.getSpecs());
        } else {
            queryWrapper.isNull("specs");
        }

        Cart existingCart = cartService.getOne(queryWrapper);

        if (existingCart != null) {
            // 已存在，更新数量
            existingCart.setQuantity(existingCart.getQuantity() + cart.getQuantity());
            cartService.updateById(existingCart);
        } else {
            // 不存在，新增
            cartService.save(cart);
        }

        return ResponseUtils.success(true);
    }

    @GetMapping("/list")
    public ResponseDTO<List<CartVO>> getCartList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "请先登录");
        }

        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");

        List<Cart> cartList = cartService.list(queryWrapper);

        // 转换为VO，关联商品信息
        List<CartVO> cartVOList = cartList.stream().map(cart -> {
            CartVO cartVO = new CartVO();
            BeanUtils.copyProperties(cart, cartVO);

            // 获取商品信息
            Product product = productService.getById(cart.getProductId());
            if (product != null) {
                cartVO.setProduct(product);
                // 计算小计
                if (product.getPrice() != null && cart.getQuantity() != null) {
                    cartVO.setSubtotal(product.getPrice().multiply(java.math.BigDecimal.valueOf(cart.getQuantity())));
                }
            }

            return cartVO;
        }).collect(Collectors.toList());

        return ResponseUtils.success(cartVOList);
    }

    @PostMapping("/update")
    public ResponseDTO<Boolean> updateCart(@RequestBody Cart cart, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "请先登录");
        }

        // 校验是否属于当前用户
        Cart existingCart = cartService.getById(cart.getId());
        if (existingCart == null || !existingCart.getUserId().equals(loginUser.getId())) {
            return ResponseUtils.error(403, "无权限");
        }

        // 校验库存
        if (cart.getQuantity() != null && cart.getQuantity() > 0) {
            Product product = productService.getById(existingCart.getProductId());
            if (product != null && product.getStock() != null && product.getStock() < cart.getQuantity()) {
                return ResponseUtils.error(400, "库存不足");
            }
        }

        cartService.updateById(cart);
        return ResponseUtils.success(true);
    }

    @PostMapping("/delete")
    public ResponseDTO<Boolean> deleteCart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "请先登录");
        }

        Cart cart = cartService.getById(deleteRequest.getId());
        if (cart == null || !cart.getUserId().equals(loginUser.getId())) {
            return ResponseUtils.error(403, "无权限");
        }

        cartService.removeById(deleteRequest.getId());
        return ResponseUtils.success(true);
    }

    @PostMapping("/clear")
    public ResponseDTO<Boolean> clearCart(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "请先登录");
        }

        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("is_delete", 0);

        cartService.remove(queryWrapper);
        return ResponseUtils.success(true);
    }
}

