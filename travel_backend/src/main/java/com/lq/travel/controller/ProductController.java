package com.lq.travel.controller;

import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.product.ProductSaveRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.ProductVO;
import com.lq.travel.service.ProductService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private UserService userService;

    @GetMapping("/my")
    @AuthCheck
    public ResponseDTO<List<ProductVO>> getMyProducts(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResponseUtils.success(productService.listUserProducts(loginUser.getId()));
    }

    @GetMapping("/{id}")
    @AuthCheck
    public ResponseDTO<ProductVO> getProductById(@PathVariable Long id,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResponseUtils.success(productService.getUserProductById(id, loginUser.getId()));
    }

    @PostMapping("/save")
    @AuthCheck
    public ResponseDTO<Long> saveProduct(@RequestBody ProductSaveRequest request,
                                         HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        return ResponseUtils.success(productService.saveProduct(request, loginUser));
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public ResponseDTO<Boolean> deleteProduct(@PathVariable Long id,
                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        productService.deleteProduct(id, loginUser.getId());
        return ResponseUtils.success(true);
    }
}
