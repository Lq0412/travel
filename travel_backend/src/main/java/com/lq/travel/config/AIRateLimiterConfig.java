package com.lq.travel.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 限流配置
 * 防止API被滥用
 */
@Slf4j
@Configuration
public class AIRateLimiterConfig {
    
    /**
     * 创建限流注册表
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
    
    /**
     * AI聊天接口限流器
     * 每分钟最多10次请求
     */
    @Bean
    public RateLimiter aiChatRateLimiter(RateLimiterRegistry registry) {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(10)                    // 每个周期最多10次请求
                .limitRefreshPeriod(Duration.ofSeconds(60))  // 周期为60秒
                .timeoutDuration(Duration.ofMillis(100))     // 获取许可的超时时间
                .build();
        
        RateLimiter rateLimiter = registry.rateLimiter("aiChat", config);
        log.info("AI聊天限流器初始化完成 - 限制: 10次/分钟");
        return rateLimiter;
    }
    
    /**
     * AI流式接口限流器
     * 每分钟最多5次请求（流式接口更消耗资源）
     */
    @Bean
    public RateLimiter aiStreamRateLimiter(RateLimiterRegistry registry) {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
        
        RateLimiter rateLimiter = registry.rateLimiter("aiStream", config);
        log.info("AI流式限流器初始化完成 - 限制: 5次/分钟");
        return rateLimiter;
    }
    
    /**
     * 代理接口限流器
     * 每分钟最多3次请求（代理通常需要多轮对话）
     */
    @Bean
    public RateLimiter aiAgentRateLimiter(RateLimiterRegistry registry) {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
        
        RateLimiter rateLimiter = registry.rateLimiter("aiAgent", config);
        log.info("AI代理限流器初始化完成 - 限制: 3次/分钟");
        return rateLimiter;
    }
}

