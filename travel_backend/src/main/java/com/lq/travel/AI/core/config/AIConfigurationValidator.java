package com.lq.travel.AI.core.config;

import com.lq.travel.AI.core.provider.DashScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AI配置验证器
 * 在应用启动时验证AI相关配置是否正确
 */
@Slf4j
@Component
public class AIConfigurationValidator {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired(required = false)
    private DashScopeProvider dashScopeProvider;
    
    @Value("${spring.ai.dashscope.chat.options.model}")
    private String defaultModel;
    
    @Value("${spring.ai.dashscope.chat.options.temperature}")
    private Double temperature;
    
    @Value("${spring.ai.dashscope.chat.options.max-tokens}")
    private Integer maxTokens;
    
    @Value("${spring.ai.dashscope.chat.options.timeout}")
    private Integer timeout;
    
    @Value("${spring.ai.dashscope.chat.options.max-retries}")
    private Integer maxRetries;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.debug("开始验证AI配置...");
        
        // 验证API密钥
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.error("通义千问API密钥未配置！请在配置文件中设置 spring.ai.dashscope.api-key");
        } else if (!dashScopeApiKey.startsWith("sk-")) {
            log.warn("通义千问API密钥格式可能不正确，通常以'sk-'开头");
        } else {
            log.debug("通义千问API密钥配置正确");
        }
        
        // 验证模型配置
        String[] validModels = dashScopeProvider != null ? dashScopeProvider.getAvailableModels() : null;
        if (validModels == null || validModels.length == 0) {
            validModels = new String[]{"qwen-plus", "qwen-turbo", "qwen-max", "qwen-long", "qwen-7b-chat", "qwen-14b-chat"};
        }
        boolean validModel = false;
        if (defaultModel != null) {
            validModel = Arrays.asList(validModels).contains(defaultModel);
        }
        
        if (!validModel) {
            log.warn("默认模型 '{}' 可能不是有效的通义千问模型", defaultModel);
        } else {
            log.debug("默认模型配置正确: {}", defaultModel);
        }
        
        // 验证温度参数
        if (temperature < 0.0 || temperature > 2.0) {
            log.warn("温度参数 {} 超出推荐范围 [0.0, 2.0]", temperature);
        } else {
            log.debug("温度参数配置正确: {}", temperature);
        }
        
        // 验证最大token数
        if (maxTokens < 1 || maxTokens > 10000) {
            log.warn("最大token数 {} 超出推荐范围 [1, 10000]", maxTokens);
        } else {
            log.debug("最大token数配置正确: {}", maxTokens);
        }
        
        // 验证超时配置
        if (timeout < 10 || timeout > 600) {
            log.warn("超时时间 {} 秒超出推荐范围 [10, 600]", timeout);
        } else {
            log.debug("超时时间配置正确: {} 秒", timeout);
        }
        
        // 验证重试次数
        if (maxRetries < 0 || maxRetries > 10) {
            log.warn("最大重试次数 {} 超出推荐范围 [0, 10]", maxRetries);
        } else {
            log.debug("最大重试次数配置正确: {}", maxRetries);
        }

        log.info("AI配置验证完成");
    }
}
