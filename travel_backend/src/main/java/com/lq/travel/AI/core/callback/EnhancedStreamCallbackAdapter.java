package com.lq.travel.AI.core.callback;

import com.lq.travel.AI.core.interfaces.StreamCallback;
import com.lq.travel.AI.core.model.IntentType;
import com.lq.travel.AI.core.util.StructuredItineraryExtractor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 增强的流式回调适配器
 * 支持结构化数据提取和处理，能够识别AI返回的JSON数据并添加特殊标记
 */
@Slf4j
public class EnhancedStreamCallbackAdapter implements StreamCallback {
    
    private final StreamCallback delegate;
    private final IntentType intent;
    private final StringBuilder fullResponse = new StringBuilder();
    private boolean structuredDataSent = false;
    
    public EnhancedStreamCallbackAdapter(StreamCallback delegate, IntentType intent) {
        this.delegate = delegate;
        this.intent = intent;
    }
    
    @Override
    public void onData(String data) {
        fullResponse.append(data);

        // 正常文本数据直接转发，避免中途截断模型输出。
        delegate.onData(data);
    }
    
    @Override
    public void onComplete() {
        // 在流式结束时统一提取结构化数据，保证 JSON 提取完整性。
        if (!structuredDataSent && intent == IntentType.ITINERARY_GENERATION) {
            Optional<String> normalizedJson = StructuredItineraryExtractor.extractAndNormalize(fullResponse.toString());
            if (normalizedJson.isPresent()) {
                try {
                    delegate.onData("\n__STRUCTURED_DATA_START__\n");
                    delegate.onData(normalizedJson.get());
                    delegate.onData("\n__STRUCTURED_DATA_END__\n");
                    structuredDataSent = true;
                    log.info("已提取并发送标准化结构化数据，长度: {} 字符", normalizedJson.get().length());
                } catch (Exception e) {
                    log.error("发送结构化数据失败", e);
                }
            } else {
                log.warn("流式响应完成，但未能提取到有效的JSON数据");
            }
        }
        
        log.info("流式响应完成 - 意图: {}, 总长度: {} 字符, 已发送结构化数据: {}", 
                intent.getDescription(), fullResponse.length(), structuredDataSent);
        delegate.onComplete();
    }
    
    @Override
    public void onError(Exception error) {
        log.error("流式响应错误 - 意图: {}", intent.getDescription(), error);
        delegate.onError(error);
    }
    
    /**
     * 获取完整的响应内容
     */
    public String getFullResponse() {
        return fullResponse.toString();
    }
    
    /**
     * 是否已发送结构化数据
     */
    public boolean isStructuredDataSent() {
        return structuredDataSent;
    }
}
