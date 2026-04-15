package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.ProductMapper;
import com.lq.travel.model.dto.product.ProductSaveRequest;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.ProductVO;
import com.lq.travel.service.AmapService;
import com.lq.travel.service.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AmapService amapService;

    @Override
    @Transactional
    public List<ProductVO> listUserProducts(Long userId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("is_delete", 0)
                .orderByAsc("id");
        return list(queryWrapper).stream().map(this::toProductVO).toList();
    }

    @Override
    @Transactional
    public ProductVO getUserProductById(Long productId, Long userId) {
        Product product = getByIdAndUser(productId, userId);
        return toProductVO(product);
    }

    @Override
    @Transactional
    public Long saveProduct(ProductSaveRequest request, User user) {
        validateRequest(request);

        Product product;
        if (request.getId() == null) {
            product = new Product();
            product.setUserId(user.getId());
        } else {
            product = getByIdAndUser(request.getId(), user.getId());
        }

        product.setName(request.getName().trim());
        product.setCity(request.getCity().trim());
        product.setAddress(request.getAddress().trim());
        product.setTagsJson(writeTags(request.getTags()));
        product.setDescription(request.getDescription());
        product.setIsRecommendable(Boolean.TRUE.equals(request.getIsRecommendable()) ? 1 : 0);
        product.setIsPurchasable(Boolean.TRUE.equals(request.getIsPurchasable()) ? 1 : 0);
        product.setCover(request.getCover().trim());
        applyGeoLocation(product, request);

        boolean result = saveOrUpdate(product);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "商品保存失败");
        }
        return product.getId();
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        Product product = getByIdAndUser(productId, userId);
        boolean result = removeById(product.getId());
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "商品删除失败");
        }
    }

    private Product getByIdAndUser(Long productId, Long userId) {
        Product product = lambdaQuery()
                .eq(Product::getId, productId)
                .eq(Product::getUserId, userId)
                .eq(Product::getIsDelete, 0)
                .one();
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        return product;
    }

    private void validateRequest(ProductSaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品名称不能为空");
        }
        if (!StringUtils.hasText(request.getCity())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属城市不能为空");
        }
        if (!StringUtils.hasText(request.getAddress())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品地址不能为空");
        }
        if (!StringUtils.hasText(request.getCover())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品封面不能为空");
        }
    }

    private ProductVO toProductVO(Product product) {
        ProductVO productVO = new ProductVO();
        productVO.setId(product.getId());
        productVO.setName(product.getName());
        productVO.setCity(product.getCity());
        productVO.setAddress(product.getAddress());
        productVO.setTags(readTags(product.getTagsJson()));
        productVO.setDescription(product.getDescription());
        productVO.setIsRecommendable(product.getIsRecommendable() != null && product.getIsRecommendable() == 1);
        productVO.setIsPurchasable(product.getIsPurchasable() != null && product.getIsPurchasable() == 1);
        productVO.setCover(product.getCover());
        productVO.setLatitude(product.getLatitude());
        productVO.setLongitude(product.getLongitude());
        productVO.setCreateTime(product.getCreateTime());
        productVO.setUpdateTime(product.getUpdateTime());
        return productVO;
    }

    private void applyGeoLocation(Product product, ProductSaveRequest request) {
        AmapService.GeoPoint geoPoint = amapService == null ? null : amapService.geocode(request.getCity(), request.getAddress());
        if (geoPoint != null) {
            product.setAddress(StringUtils.hasText(geoPoint.formattedAddress()) ? geoPoint.formattedAddress() : request.getAddress().trim());
            product.setLongitude(geoPoint.longitude());
            product.setLatitude(geoPoint.latitude());
            return;
        }

        product.setLongitude(request.getLongitude());
        product.setLatitude(request.getLatitude());
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品标签序列化失败");
        }
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("商品标签反序列化失败, tagsJson={}", tagsJson, e);
            return new ArrayList<>();
        }
    }
}
