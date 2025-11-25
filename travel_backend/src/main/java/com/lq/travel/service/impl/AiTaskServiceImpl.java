package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.service.AiTaskService;
import com.lq.travel.mapper.AiTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * AI 任务服务实现
 */
@Slf4j
@Service
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask> implements AiTaskService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Override
    public AiTask createTask(String taskId, String bizKey, String type, String payload, Long userId, Long tripId, Integer maxRetry) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(type)) {
            throw new IllegalArgumentException("taskId/type is required");
        }
        // 幂等：按 bizKey 返回已有任务
        if (StringUtils.hasText(bizKey)) {
            Optional<AiTask> existing = findByBizKey(bizKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setBizKey(bizKey);
        task.setType(type);
        task.setPayload(payload);
        task.setStatus(STATUS_PENDING);
        task.setRetryCount(0);
        task.setMaxRetry(maxRetry != null ? maxRetry : 3);
        task.setUserId(userId);
        task.setTripId(tripId);
        this.save(task);
        return task;
    }

    @Override
    public Optional<AiTask> findByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return Optional.empty();
        }
        AiTask task = this.getOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .last("limit 1"));
        return Optional.ofNullable(task);
    }

    @Override
    public Optional<AiTask> findByBizKey(String bizKey) {
        if (!StringUtils.hasText(bizKey)) {
            return Optional.empty();
        }
        AiTask task = this.getOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getBizKey, bizKey)
                .last("limit 1"));
        return Optional.ofNullable(task);
    }

    @Override
    public boolean markRunning(String taskId) {
        return findByTaskId(taskId).map(task -> {
            if (STATUS_PENDING.equals(task.getStatus())) {
                task.setStatus(STATUS_RUNNING);
                task.setErrorMessage(null);
                return this.updateById(task);
            }
            return false;
        }).orElse(false);
    }

    @Override
    public boolean markSuccess(String taskId, String result) {
        return findByTaskId(taskId).map(task -> {
            task.setStatus(STATUS_SUCCESS);
            task.setResult(result);
            task.setErrorMessage(null);
            return this.updateById(task);
        }).orElse(false);
    }

    @Override
    public boolean markFailed(String taskId, String errorMessage) {
        return findByTaskId(taskId).map(task -> {
            task.setStatus(STATUS_FAILED);
            task.setErrorMessage(errorMessage);
            Integer retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
            task.setRetryCount(retry);
            return this.updateById(task);
        }).orElse(false);
    }

    @Override
    public boolean updateResult(String taskId, String result) {
        return findByTaskId(taskId).map(task -> {
            task.setResult(result);
            return this.updateById(task);
        }).orElse(false);
    }
}
