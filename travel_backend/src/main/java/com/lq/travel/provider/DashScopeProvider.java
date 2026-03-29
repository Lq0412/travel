package com.lq.travel.provider;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.lq.travel.provider.AIProvider;
import com.lq.travel.callback.StreamCallback;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

/**
 * 通义千问AI提供商实现
 * 使用阿里云通义千问原生SDK
 */
@Slf4j
@Component
public class DashScopeProvider implements AIProvider {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.dashscope.chat.options.model}")
    private String defaultModel;
    
    @Value("${spring.ai.dashscope.chat.options.temperature}")
    private Double defaultTemperature;
    
    @Value("${spring.ai.dashscope.chat.options.max-tokens}")
    private Integer defaultMaxTokens;
    
    @Value("${spring.ai.dashscope.chat.options.timeout}")
    private Integer timeoutSeconds;
    
    @Value("${spring.ai.dashscope.chat.options.max-retries}")
    private Integer maxRetries;
    
    /**
     * 异步执行器，用于流式调用
     */
    private final Executor executor = Executors.newCachedThreadPool();
    
    /**
     * 初始化API密钥
     */
    @PostConstruct
    public void initApiKey() {
        log.debug("初始化DashScopeProvider，API密钥: {}, 默认模型: {}", 
                apiKey != null ? "已配置" : "未配置", defaultModel);
        
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            Constants.apiKey = apiKey;
            log.debug("通义千问API密钥已设置");
        } else {
            log.error("通义千问API密钥未配置");
        }
    }
    
    @Override
    public String getProviderName() {
        return "dashscope";
    }
    
    @Override
    public boolean isAvailable() {
        boolean available = apiKey != null && !apiKey.trim().isEmpty();
        log.debug("DashScope提供商可用性检查: {}, API密钥: {}, 默认模型: {}, 超时: {}秒, 最大重试: {}次", 
                available, apiKey != null ? "已配置" : "未配置", defaultModel, timeoutSeconds, maxRetries);
        return available;
    }
    
    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 创建Generation对象
            Generation generation = new Generation();
            
            // 2. 确定使用的模型（优先使用请求中的模型，否则使用默认模型）
            String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
            
            // 3. 验证模型参数
            if (modelToUse == null || modelToUse.trim().isEmpty()) {
                log.error("模型参数为空，请求模型: {}, 默认模型: {}", request.getModel(), defaultModel);
                return AIResponse.error("模型参数不能为空", getProviderName());
            }
            
                log.debug("发送请求到通义千问，请求模型: {}, 默认模型: {}, 使用模型: {}, 消息长度: {}", 
                    request.getModel(), defaultModel, modelToUse, request.getMessage().length());
            
            // 4. 构建消息列表（包含系统提示词、历史消息、当前消息）
            List<Message> messages = buildDashScopeMessages(request);
            
            // 5. 创建请求参数
            GenerationParam param = GenerationParam.builder()
                    .model(modelToUse)                    // 指定AI模型
                    .messages(messages)                   // 消息列表
                    .temperature(request.getTemperature() != null ? 
                        request.getTemperature().floatValue() : defaultTemperature.floatValue())  // 温度参数
                    .maxTokens(request.getMaxTokens() != null ? 
                        request.getMaxTokens() : defaultMaxTokens)  // 最大Token数
                    .build();
            
            // 6. 调用通义千问API（非流式调用）
            GenerationResult result = generation.call(param);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 7. 处理响应结果
            if (result != null && result.getOutput() != null && result.getOutput().getText() != null) {
                String content = result.getOutput().getText();
                if (content.trim().isEmpty()) {
                    log.warn("通义千问返回空内容，模型: {}, 响应时间: {}ms", modelToUse, responseTime);
                    return AIResponse.error("通义千问返回空内容", getProviderName());
                }
                
                log.debug("通义千问调用成功，模型: {}, 响应时间: {}ms, Token使用: {}", 
                        modelToUse, responseTime, 
                        result.getUsage() != null ? result.getUsage().getTotalTokens() : "未知");
                
                // 8. 构建成功响应
                AIResponse response = AIResponse.success(content, modelToUse, getProviderName());
                response.setTokensUsed(result.getUsage() != null ? result.getUsage().getTotalTokens() : null);
                response.setResponseTime(responseTime);
                return response;
            } else {
                log.error("通义千问响应为空，模型: {}, 响应时间: {}ms", modelToUse, responseTime);
                return AIResponse.error("通义千问响应为空", getProviderName());
            }
            
        } catch (NoApiKeyException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("通义千问API密钥缺失，模型: {}, 响应时间: {}ms", 
                    request.getModel() != null ? request.getModel() : defaultModel, responseTime, e);
            AIResponse response = AIResponse.error("API密钥缺失，请检查配置", getProviderName());
            response.setResponseTime(responseTime);
            return response;
        } catch (InputRequiredException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("通义千问输入参数缺失，模型: {}, 响应时间: {}ms", 
                    request.getModel() != null ? request.getModel() : defaultModel, responseTime, e);
            AIResponse response = AIResponse.error("输入参数缺失: " + e.getMessage(), getProviderName());
            response.setResponseTime(responseTime);
            return response;
        } catch (ApiException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("通义千问API调用异常，模型: {}, 响应时间: {}ms, 错误信息: {}", 
                    request.getModel() != null ? request.getModel() : defaultModel, responseTime, 
                    e.getMessage(), e);
            AIResponse response = AIResponse.error("API调用失败: " + e.getMessage(), getProviderName());
            response.setResponseTime(responseTime);
            return response;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("通义千问调用异常，模型: {}, 响应时间: {}ms, 错误: {}", 
                    request.getModel() != null ? request.getModel() : defaultModel, responseTime, e.getMessage(), e);
            AIResponse response = AIResponse.error("调用异常: " + e.getMessage(), getProviderName());
            response.setResponseTime(responseTime);
            return response;
        }
    }
    
    @Override
    public void chatStream(AIRequest request, StreamCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Generation generation = new Generation();
                List<Message> messages = buildDashScopeMessages(request);
                String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
                
                log.debug("开始流式调用通义千问，模型: {}, 消息长度: {}", modelToUse, request.getMessage().length());
                
                // 构建请求参数，启用增量输出
                GenerationParam param = GenerationParam.builder()
                        .model(modelToUse)
                        .messages(messages)
                        .temperature(request.getTemperature() != null ? 
                            request.getTemperature().floatValue() : defaultTemperature.floatValue())
                        .maxTokens(request.getMaxTokens() != null ? 
                            request.getMaxTokens() : defaultMaxTokens)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)  // 开启增量输出，流式返回
                        .build();
                
                // 发起流式调用
                Flowable<GenerationResult> result = generation.streamCall(param);
                
                // 使用原子变量防止重复完成
                final AtomicBoolean completed = new AtomicBoolean(false);
                
                result
                    .subscribeOn(Schedulers.io()) // IO线程执行请求
                    .observeOn(Schedulers.computation()) // 计算线程处理响应
                    .subscribe(
                        // onNext: 处理每个响应片段
                        message -> {
                            try {
                                if (message.getOutput() != null && 
                                    message.getOutput().getChoices() != null && 
                                    !message.getOutput().getChoices().isEmpty()) {
                                    
                                    String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                                    String finishReason = message.getOutput().getChoices().get(0).getFinishReason();
                                    
                                    // 输出内容
                                    if (content != null && !content.isEmpty()) {
                                        log.debug("收到流式数据: {}", content);
                                        callback.onData(content);
                                    }
                                    
                                    // 当 finishReason 不为 null 时，表示是最后一个 chunk
                                    if (finishReason != null && !"null".equals(finishReason)) {
                                        log.debug("通义千问流式调用完成，模型: {}, 输入Tokens: {}, 输出Tokens: {}, 总Tokens: {}", 
                                                modelToUse,
                                                message.getUsage() != null ? message.getUsage().getInputTokens() : 0,
                                                message.getUsage() != null ? message.getUsage().getOutputTokens() : 0,
                                                message.getUsage() != null ? message.getUsage().getTotalTokens() : 0);
                                        
                                        // 使用原子变量确保只完成一次
                                        if (completed.compareAndSet(false, true)) {
                                            callback.onComplete();
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("处理流式响应片段时出错", e);
                                if (completed.compareAndSet(false, true)) {
                                    callback.onError(e);
                                }
                            }
                        },
                        // onError: 处理错误
                        error -> {
                            log.error("DashScope流式调用异常，模型: {}", modelToUse, error);
                            if (completed.compareAndSet(false, true)) {
                                if (error instanceof Exception) {
                                    callback.onError((Exception) error);
                                } else {
                                    callback.onError(new RuntimeException(error.getMessage(), error));
                                }
                            }
                        },
                        // onComplete: 完成回调（如果没有在onNext中调用）
                        () -> {
                            log.debug("通义千问流式调用自然完成，模型: {}", modelToUse);
                            // 使用原子变量确保只完成一次
                            if (completed.compareAndSet(false, true)) {
                                callback.onComplete();
                            }
                        }
                    );
                    
            } catch (Exception e) {
                log.error("流式请求构建失败", e);
                callback.onError(e);
            }
        }, executor);  // 使用指定的线程池
    }
    
    @Override
    public String[] getAvailableModels() {
        return new String[]{
            "tongyi-xiaomi-analysis-pro",

        };
    }
    
    @Override
    public String getDefaultModel() {
        return defaultModel;
    }
    
    /**
     * 构建通义千问格式的消息列表
     */
    private List<Message> buildDashScopeMessages(AIRequest request) {
        List<Message> messages = new ArrayList<>();
        
        // 添加系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(Message.builder()
                    .role("system")
                    .content(request.getSystemPrompt())
                    .build());
        }
        
        // 添加历史消息
        if (request.getHistory() != null) {
            for (AIRequest.Message msg : request.getHistory()) {
                String role = switch (msg.getRole()) {
                    case "system" -> "system";
                    case "assistant" -> "assistant";
                    default -> "user";
                };
                
                messages.add(Message.builder()
                        .role(role)
                        .content(msg.getContent())
                        .build());
            }
        }
        
        // 添加当前用户消息
        messages.add(Message.builder()
                .role("user")
                .content(request.getMessage())
                .build());
        
        return messages;
    }
}
