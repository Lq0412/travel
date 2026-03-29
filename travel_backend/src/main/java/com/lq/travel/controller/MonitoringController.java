package com.lq.travel.controller;

import com.lq.travel.service.AIUserContextService;
import com.lq.travel.service.QuotaService;
import com.lq.travel.util.AICacheHandler;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模块监控端点
 * 提供健康检查、统计信息、配额查询等功能
 */
@Slf4j
@RestController
@RequestMapping("/ai/monitor")
public class MonitoringController {

    @Resource
    private AICacheHandler cacheHandler;

    @Resource
    private QuotaService quotaService;

    @Resource
    private AIUserContextService aiUserContextService;

    /**
     * 健康检查端点
     * 公开接口，无需登录
     */
    @GetMapping("/health")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Module");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ResponseDTO.success("AI模块运行正常", health));
    }

    /**
     * 缓存统计信息
     * 需要管理员权限
     */
    @GetMapping("/cache/stats")
    @AuthCheck(mustRole = "admin")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> cacheStats() {
        Map<String, Object> stats = cacheHandler.getComprehensiveStats();
        return ResponseEntity.ok(ResponseDTO.success("获取缓存统计成功", stats));
    }

    /**
     * 线程池统计信息
     * 需要管理员权限
     */
    @GetMapping("/executor/stats")
    @AuthCheck(mustRole = "admin")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> executorStats() {
        Map<String, Object> stats = cacheHandler.getExecutorStats();
        return ResponseEntity.ok(ResponseDTO.success("获取线程池统计成功", stats));
    }

    /**
     * 用户配额查询
     * 用户只能查询自己的配额
     */
    @GetMapping("/quota")
    @AuthCheck(mustRole = "user")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getQuota(HttpServletRequest request) {
        Long loginUserId = aiUserContextService.extractLoginUserId(request);
        if (loginUserId == null) {
            return ResponseEntity.ok(ResponseDTO.error("请先登录"));
        }

        Integer remaining = quotaService.getRemainingQuota(loginUserId);

        Map<String, Object> quotaInfo = new HashMap<>();
        quotaInfo.put("userId", loginUserId);
        quotaInfo.put("remainingQuota", remaining);
        quotaInfo.put("unit", "tokens");
        quotaInfo.put("resetTime", "每天0点重置");

        return ResponseEntity.ok(ResponseDTO.success("获取配额成功", quotaInfo));
    }

    /**
     * 清除用户对话缓存
     * 用户可以清除自己的缓存
     */
    @DeleteMapping("/cache/conversation/{conversationId}")
    @AuthCheck(mustRole = "user")
    public ResponseEntity<ResponseDTO<Void>> clearConversationCache(
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        Long loginUserId = aiUserContextService.extractLoginUserId(request);
        if (loginUserId == null) {
            return ResponseEntity.ok(ResponseDTO.error("请先登录"));
        }

        // TODO: 验证conversationId是否属于该用户
        cacheHandler.clearConversationCache(conversationId);
        log.info("用户 {} 清除了对话 {} 的缓存", loginUserId, conversationId);

        return ResponseEntity.ok(ResponseDTO.success("清除缓存成功", null));
    }

    /**
     * 管理员：清除所有缓存
     * 需要管理员权限
     */
    @DeleteMapping("/cache/all")
    @AuthCheck(mustRole = "admin")
    public ResponseEntity<ResponseDTO<Void>> clearAllCache() {
        cacheHandler.clearAllCache();
        log.info("管理员清除了所有AI缓存");

        return ResponseEntity.ok(ResponseDTO.success("清除所有缓存成功", null));
    }

    /**
     * 管理员：重置用户配额
     * 需要管理员权限
     */
    @PutMapping("/quota/reset")
    @AuthCheck(mustRole = "admin")
    public ResponseEntity<ResponseDTO<Void>> resetUserQuota(
            @RequestParam Long userId,
            @RequestParam Integer quota) {

        if (quota == null || quota < 0) {
            return ResponseEntity.ok(ResponseDTO.error("配额值无效"));
        }

        quotaService.resetQuota(userId, quota);
        log.info("管理员重置用户 {} 的配额为: {}", userId, quota);

        return ResponseEntity.ok(ResponseDTO.success("重置配额成功", null));
    }

    /**
     * 管理员：为用户充值配额
     * 需要管理员权限
     */
    @PostMapping("/quota/recharge")
    @AuthCheck(mustRole = "admin")
    public ResponseEntity<ResponseDTO<Void>> rechargeQuota(
            @RequestParam Long userId,
            @RequestParam Integer tokens) {

        if (tokens == null || tokens <= 0) {
            return ResponseEntity.ok(ResponseDTO.error("充值数量无效"));
        }

        quotaService.rechargeQuota(userId, tokens);
        log.info("管理员为用户 {} 充值 {} tokens", userId, tokens);

        return ResponseEntity.ok(ResponseDTO.success("充值成功", null));
    }
}
