package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.AiTask;

import java.util.Optional;

/**
 * AI 任务服务
 */
public interface AiTaskService extends IService<AiTask> {

    /**
    * 创建任务，带幂等校验（若 bizKey 存在则返回已有记录）
    */
    AiTask createTask(String taskId, String bizKey, String type, String payload, Long userId, Long tripId, Integer maxRetry);

    Optional<AiTask> findByTaskId(String taskId);

    Optional<AiTask> findByBizKey(String bizKey);

    boolean markRunning(String taskId);

    boolean markSuccess(String taskId, String result);

    boolean markFailed(String taskId, String errorMessage);

    /**
     * 更新结果，不改变状态
     */
    boolean updateResult(String taskId, String result);
}
