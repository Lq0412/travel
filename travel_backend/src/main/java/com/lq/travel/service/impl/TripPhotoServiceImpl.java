package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.TripPhotoMapper;
import com.lq.travel.model.entity.Trip;
import com.lq.travel.model.entity.TripPhoto;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripPhotoVO;
import com.lq.travel.service.TripPhotoService;
import com.lq.travel.service.TripService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行程照片服务实现类
 */
@Slf4j
@Service
public class TripPhotoServiceImpl extends ServiceImpl<TripPhotoMapper, TripPhoto> implements TripPhotoService {
    
    @Resource
    private TripService tripService;
    
    @Override
    @Transactional
    public TripPhotoVO uploadPhoto(Long tripId, String photoUrl, User user) {
        // 验证行程是否存在且属于该用户
        Trip trip = tripService.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }
        
        // 检查照片数量限制（3-6张）
        QueryWrapper<TripPhoto> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", tripId);
        queryWrapper.eq("is_delete", 0);
        long photoCount = this.count(queryWrapper);
        
        if (photoCount >= 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多只能上传6张照片");
        }
        
        // 创建照片记录
        TripPhoto photo = new TripPhoto();
        photo.setTripId(tripId);
        photo.setPhotoUrl(photoUrl);
        photo.setShotTime(new Date());
        photo.setSortOrder((int) (photoCount + 1));
        
        this.save(photo);
        
        return convertToVO(photo);
    }
    
    @Override
    @Transactional
    public List<TripPhotoVO> uploadPhotos(Long tripId, List<String> photoUrls, User user) {
        // 验证行程
        Trip trip = tripService.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }
        
        // 检查照片数量限制
        QueryWrapper<TripPhoto> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", tripId);
        queryWrapper.eq("is_delete", 0);
        long existingCount = this.count(queryWrapper);
        
        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "照片列表不能为空");
        }
        
        if (photoUrls.size() > 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多只能上传6张照片");
        }
        
        if (existingCount + photoUrls.size() > 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "照片总数不能超过6张");
        }
        
        // 批量保存照片
        Date now = new Date();
        List<TripPhoto> photos = new java.util.ArrayList<>();
        for (int i = 0; i < photoUrls.size(); i++) {
            TripPhoto photo = new TripPhoto();
            photo.setTripId(tripId);
            photo.setPhotoUrl(photoUrls.get(i));
            photo.setShotTime(now);
            photo.setSortOrder((int) (existingCount + i + 1));
            photos.add(photo);
        }
        
        this.saveBatch(photos);
        
        return photos.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<TripPhotoVO> getTripPhotos(Long tripId) {
        QueryWrapper<TripPhoto> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", tripId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByAsc("sort_order");
        
        List<TripPhoto> photos = this.list(queryWrapper);
        
        return photos.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deletePhoto(Long photoId, Long userId) {
        TripPhoto photo = this.getById(photoId);
        if (photo == null || photo.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "照片不存在");
        }
        
        // 验证行程所有权
        Trip trip = tripService.getById(photo.getTripId());
        if (trip == null || !trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该照片");
        }
        
        this.removeById(photoId);
    }
    
    /**
     * 转换为VO对象
     */
    private TripPhotoVO convertToVO(TripPhoto photo) {
        TripPhotoVO vo = new TripPhotoVO();
        BeanUtils.copyProperties(photo, vo);
        return vo;
    }
}

