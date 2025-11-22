package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.model.entity.Trip;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程Mapper
 */
@Mapper
public interface TripMapper extends BaseMapper<Trip> {
}

