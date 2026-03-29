package com.lq.travel.AI.service.impl;

import com.lq.travel.AI.service.AIUserContextService;
import com.lq.travel.AI.service.QuotaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 配额管理服务实现
 */
@Slf4j
@Service
public class QuotaServiceImpl implements QuotaService {
    
    @Resource
    private RedisTemplate<String, Integer> redisTemplate;
    
    @Resource
    private AIUserContextService aiUserContextService;
    
    /**
     * 配额检查开关
     */
    @Value("${ai.quota.enabled:true}")
    private boolean quotaEnabled;
    
    /**
     * 每日默认配额（Token数量）
     */
    private static final int DAILY_QUOTA = 10000;
    
    /**
     * 配额键前缀
     */
    private static final String QUOTA_KEY_PREFIX = "user:quota:";
    
    /**
     * 使用记录键前缀
     */
    private static final String USAGE_KEY_PREFIX = "user:usage:";
    
    @Override
    public boolean checkQuota(Long userId) {
        if (userId == null) {
            log.warn("userId为空，拒绝请求");
            return false;
        }
        
        // 配额检查开关关闭时直接通过
        if (!quotaEnabled) {
            log.debug("配额检查已禁用，用户 {} 直接通过", userId);
            return true;
        }
        
        // 检查用户角色：管理员无配额限制
        try {
            if (aiUserContextService.isAdmin(userId)) {
                log.info("管理员用户 {} 无配额限制，直接通过", userId);
                return true;
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，继续配额检查: {}", e.getMessage());
        }
        
        String key = QUOTA_KEY_PREFIX + userId;
        Integer remaining = redisTemplate.opsForValue().get(key);
        
        // 如果没有配额记录，初始化为每日配额
        if (remaining == null) {
            log.info("初始化用户 {} 的每日配额: {}", userId, DAILY_QUOTA);
            redisTemplate.opsForValue().set(key, DAILY_QUOTA, 24, TimeUnit.HOURS);
            return true;
        }
        
        boolean hasQuota = remaining > 0;
        if (!hasQuota) {
            log.warn("用户 {} 配额不足，剩余: {}", userId, remaining);
        }
        
        return hasQuota;
    }
    
    @Override
    public void deductQuota(Long userId, Integer tokens) {
        if (userId == null || tokens == null || tokens <= 0) {
            return;
        }
        
        String key = QUOTA_KEY_PREFIX + userId;
        Long remaining = redisTemplate.opsForValue().decrement(key, tokens);
        
        log.info("用户 {} 使用 {} tokens，剩余配额: {}", userId, tokens, remaining);
        
        // 记录使用情况（用于统计）
        recordUsage(userId, tokens);
    }
    
    @Override
    public Integer getRemainingQuota(Long userId) {
        if (userId == null) {
            return 0;
        }
        
        String key = QUOTA_KEY_PREFIX + userId;
        Integer remaining = redisTemplate.opsForValue().get(key);
        
        // 如果没有记录，返回每日配额
        return remaining != null ? remaining : DAILY_QUOTA;
    }
    
    @Override
    public void resetQuota(Long userId, Integer quota) {
        if (userId == null || quota == null || quota < 0) {
            return;
        }
        
        String key = QUOTA_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, quota, 24, TimeUnit.HOURS);
        
        log.info("已重置用户 {} 的配额为: {}", userId, quota);
    }
    
    @Override
    public void rechargeQuota(Long userId, Integer tokens) {
        if (userId == null || tokens == null || tokens <= 0) {
            return;
        }
        
        String key = QUOTA_KEY_PREFIX + userId;
        Long newQuota = redisTemplate.opsForValue().increment(key, tokens);
        
        log.info("用户 {} 充值 {} tokens，当前配额: {}", userId, tokens, newQuota);
    }
    
    /**
     * 记录使用情况
     */
    private void recordUsage(Long userId, Integer tokens) {
        String key = USAGE_KEY_PREFIX + userId + ":" + 
                     java.time.LocalDate.now().toString();
        
        redisTemplate.opsForValue().increment(key, tokens);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);  // 保留30天
    }
    
    /**
     * 每天凌晨重置所有用户配额
     * Cron: 每天 00:00:00 执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyQuota() {
        log.info("开始执行每日配额重置任务");
        
        // 注意：这里不直接删除所有键，因为Redis的配额会自动过期
        // 新的请求会自动初始化配额
        
        log.info("每日配额重置任务完成（自动过期机制）");
    }
}

