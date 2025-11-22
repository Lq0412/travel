package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.TripPhoto;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripPhotoVO;

import java.util.List;

/**
 * 行程照片服务接口
 */
public interface TripPhotoService extends IService<TripPhoto> {
    
    /**
     * 上传行程照片
     */
    TripPhotoVO uploadPhoto(Long tripId, String photoUrl, User user);
    
    /**
     * 批量上传行程照片
     */
    List<TripPhotoVO> uploadPhotos(Long tripId, List<String> photoUrls, User user);
    
    /**
     * 获取行程的所有照片
     */
    List<TripPhotoVO> getTripPhotos(Long tripId);
    
    /**
     * 删除照片
     */
    void deletePhoto(Long photoId, Long userId);
}

