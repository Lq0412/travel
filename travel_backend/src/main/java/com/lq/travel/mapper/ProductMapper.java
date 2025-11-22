package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 获取商家商品列表
     */
    @Select("SELECT * FROM product WHERE merchant_id = #{merchantId} AND status = 1 AND is_delete = 0 ORDER BY create_time DESC")
    IPage<Product> selectByMerchantId(Page<Product> page, @Param("merchantId") Long merchantId);
}