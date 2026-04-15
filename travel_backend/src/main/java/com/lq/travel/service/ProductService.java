package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.product.ProductSaveRequest;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.ProductVO;

import java.util.List;

public interface ProductService extends IService<Product> {

    List<ProductVO> listUserProducts(Long userId);

    ProductVO getUserProductById(Long productId, Long userId);

    Long saveProduct(ProductSaveRequest request, User user);

    void deleteProduct(Long productId, Long userId);
}
