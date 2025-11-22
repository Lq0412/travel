package com.lq.travel.controller;

import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripPhotoVO;
import com.lq.travel.service.TripPhotoService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行程照片控制器
 */
@RestController
@RequestMapping("/trips")
@Slf4j
public class TripPhotoController {
    
    @Resource
    private TripPhotoService tripPhotoService;
    
    @Resource
    private UserService userService;
    
    /**
     * 上传行程照片（单个）
     */
    @PostMapping("/{tripId}/photos")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<TripPhotoVO> uploadPhoto(
            @PathVariable Long tripId,
            @RequestParam String photoUrl,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        TripPhotoVO photo = tripPhotoService.uploadPhoto(tripId, photoUrl, loginUser);
        return ResponseUtils.success(photo);
    }
    
    /**
     * 批量上传行程照片
     */
    @PostMapping("/{tripId}/photos/batch")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<List<TripPhotoVO>> uploadPhotos(
            @PathVariable Long tripId,
            @RequestBody List<String> photoUrls,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<TripPhotoVO> photos = tripPhotoService.uploadPhotos(tripId, photoUrls, loginUser);
        return ResponseUtils.success(photos);
    }
    
    /**
     * 获取行程的所有照片
     */
    @GetMapping("/{tripId}/photos")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<List<TripPhotoVO>> getTripPhotos(
            @PathVariable Long tripId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<TripPhotoVO> photos = tripPhotoService.getTripPhotos(tripId);
        return ResponseUtils.success(photos);
    }
    
    /**
     * 删除照片
     */
    @DeleteMapping("/photos/{photoId}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Boolean> deletePhoto(
            @PathVariable Long photoId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        tripPhotoService.deletePhoto(photoId, loginUser.getId());
        return ResponseUtils.success(true);
    }
}

