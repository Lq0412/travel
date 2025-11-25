package com.lq.travel.controller;

import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.entity.AiTask;
import com.lq.travel.model.vo.AiTaskStatusVO;
import com.lq.travel.service.AiTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * AI 任务查询接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/tasks")
public class AiTaskController {

    @Resource
    private AiTaskService aiTaskService;

    @GetMapping("/{taskId}")
    public Object getTaskStatus(@PathVariable String taskId) {
        Optional<AiTask> taskOpt = aiTaskService.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseUtils.error(404, "Task not found");
        }
        AiTask task = taskOpt.get();
        AiTaskStatusVO vo = new AiTaskStatusVO();
        vo.setTaskId(task.getTaskId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setResult(task.getResult());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setUpdateTime(task.getUpdateTime());
        return ResponseUtils.success(vo);
    }
}
