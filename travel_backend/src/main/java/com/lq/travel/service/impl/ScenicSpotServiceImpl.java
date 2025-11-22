package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.ScenicSpotMapper;
import com.lq.travel.model.entity.ScenicSpot;
import com.lq.travel.service.ScenicSpotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScenicSpotServiceImpl extends ServiceImpl<ScenicSpotMapper, ScenicSpot>
        implements ScenicSpotService {

    @Override
    public List<ScenicSpot> getAllSpots() {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicSpot::getIsDelete, 0);
        wrapper.orderByDesc(ScenicSpot::getHeatValue);
        return this.list(wrapper);
    }
}

