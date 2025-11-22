package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.ScenicSpot;

import java.util.List;

public interface ScenicSpotService extends IService<ScenicSpot> {
    /**
     * 获取所有景点列表（仅未删除的）
     */
    List<ScenicSpot> getAllSpots();
}

