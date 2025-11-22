package com.lq.travel.controller;

import com.lq.travel.AI.core.annotation.ApiRateLimit;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.trip.MemoryCardGenerateRequest;
import com.lq.travel.model.dto.trip.MemoryCardGenerateResponse;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;
import com.lq.travel.service.MemoryCardService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 回忆图控制器
 */
@RestController
@RequestMapping("/ai/images")
@Slf4j
public class MemoryCardController {
    
    @Resource
    private MemoryCardService memoryCardService;
    
    @Resource
    private UserService userService;
    
    /**
     * 生成回忆图
     */
    @PostMapping("/memory-card")
    @AuthCheck
    @ApiRateLimit(name = "aiChat")
    public com.lq.travel.AI.core.model.dto.ResponseDTO<MemoryCardGenerateResponse> generateMemoryCard(
            @RequestBody MemoryCardGenerateRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        MemoryCardGenerateResponse response = memoryCardService.generateMemoryCard(request, loginUser);
        return ResponseUtils.success(response);
    }
    
    /**
     * 查询生成状态（轮询）
     */
    @GetMapping("/memory-card/status/{taskId}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<MemoryCardVO> getMemoryCardStatus(
            @PathVariable String taskId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        MemoryCardVO memoryCard = memoryCardService.getMemoryCardStatus(taskId, loginUser.getId());
        return ResponseUtils.success(memoryCard);
    }
    
    /**
     * 根据行程ID获取回忆图
     */
    @GetMapping("/memory-card/trip/{tripId}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<MemoryCardVO> getMemoryCardByTripId(
            @PathVariable Long tripId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        MemoryCardVO memoryCard = memoryCardService.getMemoryCardByTripId(tripId, loginUser.getId());
        return ResponseUtils.success(memoryCard);
    }

    /**
     * 重新生成回忆图（存档历史，创建新任务）
     */
    @PostMapping("/memory-card/{tripId}/regenerate")
    @AuthCheck
    @ApiRateLimit(name = "aiChat")
    public com.lq.travel.AI.core.model.dto.ResponseDTO<MemoryCardGenerateResponse> regenerate(
            @PathVariable Long tripId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        MemoryCardGenerateResponse response = ((com.lq.travel.service.impl.MemoryCardServiceImpl) memoryCardService)
                .regenerateMemoryCard(tripId, loginUser);
        return ResponseUtils.success(response);
    }

    /**
     * 历史版本列表
     */
    @GetMapping("/memory-card/history/{tripId}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<java.util.List<com.lq.travel.model.entity.MemoryCardHistory>> listHistory(
            @PathVariable Long tripId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        // 简化：直接在实现类中用 QueryWrapper 查询
        java.util.List<com.lq.travel.model.entity.MemoryCardHistory> list =
                ((com.lq.travel.service.impl.MemoryCardServiceImpl) memoryCardService)
                        .listHistoryByTripId(tripId, loginUser.getId());
        return ResponseUtils.success(list);
    }

    /**
     * 将历史版本设为当前
     */
    @PostMapping("/memory-card/history/{historyId}/set-current")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Boolean> setCurrentFromHistory(
            @PathVariable Long historyId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean ok = ((com.lq.travel.service.impl.MemoryCardServiceImpl) memoryCardService)
                .setCurrentFromHistory(historyId, loginUser.getId());
        return ResponseUtils.success(ok);
    }
}

