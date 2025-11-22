package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.model.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    // 搜索商家 - 移除@Select注解，使用XML配置
    IPage<Merchant> searchMerchants(Page<Merchant> page,
                                    @Param("name") String name,
                                    @Param("type") Integer type);

    // 获取推荐商家
    List<Merchant> selectRecommendedMerchants(@Param("limit") Integer limit);

    // 获取商家列表
    IPage<Merchant> selectMerchantList(Page<Merchant> page);
}