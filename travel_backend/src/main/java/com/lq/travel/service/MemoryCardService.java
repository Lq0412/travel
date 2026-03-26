package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.dto.trip.MemoryCardGenerateResponse;
import com.lq.travel.model.entity.MemoryCard;
import com.lq.travel.model.entity.MemoryCardHistory;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;

import java.util.List;

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

    /**
     * 重新生成回忆图
     */
    MemoryCardGenerateResponse regenerateMemoryCard(Long tripId, User user);

    /**
     * 查询历史版本
     */
    List<MemoryCardHistory> listHistoryByTripId(Long tripId, Long userId);

    /**
     * 将历史版本设为当前
     */
    boolean setCurrentFromHistory(Long historyId, Long userId);

    /**
     * 上传图片到 COS（用于消费者侧）
     */
    com.lq.travel.model.dto.file.UploadPictureResult uploadToCos(String imageUrl, String pathPrefix);

    /**
     * 消费者侧：标记回忆图处理中并记录远端 taskId
     */
    void updateMemoryCardProcessing(Long tripId, String remoteTaskId);

    /**
     * 消费者侧：标记回忆图生成成功并写历史
     */
    void updateMemoryCardSuccess(Long tripId, String cosUrl, String remoteTaskId);

    /**
     * 消费者侧：标记回忆图生成失败
     */
    void updateMemoryCardFailed(Long tripId, String errorMessage);
}

