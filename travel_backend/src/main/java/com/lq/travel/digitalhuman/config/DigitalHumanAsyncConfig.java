package com.lq.travel.digitalhuman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 数字人异步配置
 * 
 * @author Lq
 * @date 2025-01-XX
 */
@Configuration
@EnableAsync
public class DigitalHumanAsyncConfig {

    /**
     * 数字人异步执行器
     */
    public Executor digitalHumanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("digital-human-");
        executor.initialize();
        return executor;
    }
}

