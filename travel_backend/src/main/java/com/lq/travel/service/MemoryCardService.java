package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.dto.trip.MemoryCardGenerateResponse;
import com.lq.travel.model.entity.MemoryCard;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;

/**
 * 回忆图服务接口
 */
public interface MemoryCardService extends IService<MemoryCard> {
    
    /**
     * 生成回忆图（异步任务）
     */
    MemoryCardGenerateResponse generateMemoryCard(MemoryCardGenerateRequest request, User user);
    
    /**
     * 查询生成状态（轮询）
     */
    MemoryCardVO getMemoryCardStatus(String taskId, Long userId);
    
    /**
     * 根据行程ID获取回忆图
     */
    MemoryCardVO getMemoryCardByTripId(Long tripId, Long userId);
}

