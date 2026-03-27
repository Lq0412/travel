package com.lq.travel.AI.core.aop;

import com.lq.travel.AI.core.annotation.ApiRateLimit;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 限流切面
 * 拦截带有 @ApiRateLimit 注解的方法并应用限流
 */
@Slf4j
@Aspect
@Component
public class ApiRateLimitAspect {
    
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    @Around("@annotation(apiRateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, ApiRateLimit apiRateLimit) throws Throwable {
        String rateLimiterName = apiRateLimit.name();
        
        // 获取限流器
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);
        
        try {
            // 尝试获取许可
            return rateLimiter.executeSupplier(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
        } catch (RequestNotPermitted e) {
            // 限流触发
                log.warn("限流触发 - 限流器: {}, 方法: {}", 
                    rateLimiterName, joinPoint.getSignature().getName());
            throw e;
        }
    }
}

