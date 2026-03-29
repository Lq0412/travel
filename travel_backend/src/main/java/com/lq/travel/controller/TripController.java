package com.lq.travel.controller;

import com.lq.travel.annotation.ApiRateLimit;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.model.dto.trip.TripGenerateRequest;
import com.lq.travel.model.dto.trip.TripGenerateResponse;
import com.lq.travel.model.dto.trip.TripSaveRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripVO;
import com.lq.travel.service.TripService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行程控制器
 */
@RestController
@RequestMapping("/ai/trips")
@Slf4j
public class TripController {
    
    @Resource
    private TripService tripService;
    
    @Resource
    private UserService userService;
    
    
    /**
     * AI生成行程方案
     */
    @PostMapping("/generate")
    @AuthCheck
    @ApiRateLimit(name = "aiChat")
    public com.lq.travel.common.ResponseDTO<TripGenerateResponse> generateTrip(
            @RequestBody TripGenerateRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        TripGenerateResponse response = tripService.generateTripPlans(request, loginUser);
        return ResponseUtils.success(response);
    }
    
    /**
     * 保存行程（接受方案）
     */
    @PostMapping("/save")
    @AuthCheck
    public com.lq.travel.common.ResponseDTO<Long> saveTrip(
            @RequestBody TripSaveRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long tripId = tripService.saveTrip(request, loginUser);
        return ResponseUtils.success(tripId);
    }
    
    /**
     * 获取我的行程列表
     */
    @GetMapping("/my")
    @AuthCheck
    public com.lq.travel.common.ResponseDTO<List<TripVO>> getMyTrips(
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<TripVO> trips = tripService.getUserTrips(loginUser.getId());
        return ResponseUtils.success(trips);
    }
    
    /**
     * 获取行程详情
     */
    @GetMapping("/{id}")
    @AuthCheck
    public com.lq.travel.common.ResponseDTO<TripVO> getTripById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        TripVO trip = tripService.getTripById(id, loginUser.getId());
        return ResponseUtils.success(trip);
    }
    
    /**
     * 标记行程为已完成
     */
    @PostMapping("/{id}/complete")
    @AuthCheck
    public com.lq.travel.common.ResponseDTO<Boolean> completeTrip(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        tripService.completeTrip(id, loginUser.getId());
        return ResponseUtils.success(true);
    }
    
    /**
     * 删除行程
     */
    @DeleteMapping("/{id}")
    @AuthCheck
    public com.lq.travel.common.ResponseDTO<Boolean> deleteTrip(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        tripService.deleteTrip(id, loginUser.getId());
        return ResponseUtils.success(true);
    }
    
}



