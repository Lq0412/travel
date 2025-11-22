package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.trip.TripGenerateRequest;
import com.lq.travel.model.dto.trip.TripGenerateResponse;
import com.lq.travel.model.dto.trip.TripSaveRequest;
import com.lq.travel.model.entity.Trip;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripVO;

import java.util.List;

/**
 * 行程服务接口
 */
public interface TripService extends IService<Trip> {
    
    /**
     * AI生成行程方案
     */
    TripGenerateResponse generateTripPlans(TripGenerateRequest request, User user);
    
    /**
     * 保存行程（接受方案）
     */
    Long saveTrip(TripSaveRequest request, User user);
    
    /**
     * 获取用户的所有行程
     */
    List<TripVO> getUserTrips(Long userId);
    
    /**
     * 获取行程详情
     */
    TripVO getTripById(Long tripId, Long userId);
    
    /**
     * 更新行程状态为已完成
     */
    void completeTrip(Long tripId, Long userId);
    
    /**
     * 删除行程
     */
    void deleteTrip(Long tripId, Long userId);
}

