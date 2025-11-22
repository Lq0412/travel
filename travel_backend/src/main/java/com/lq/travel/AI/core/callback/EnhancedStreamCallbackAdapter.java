package com.lq.travel.AI.core.callback;

import com.lq.travel.AI.core.interfaces.StreamCallback;
import com.lq.travel.AI.core.model.IntentType;
import lombok.extern.slf4j.Slf4j;

/**
 * 增强的流式回调适配器
 * 支持结构化数据提取和处理，能够识别AI返回的JSON数据并添加特殊标记
 */
@Slf4j
public class EnhancedStreamCallbackAdapter implements StreamCallback {
    
    private final StreamCallback delegate;
    private final IntentType intent;
    private final StringBuilder fullResponse = new StringBuilder();
    private boolean inJsonBlock = false;
    private final StringBuilder jsonBuffer = new StringBuilder();
    private boolean structuredDataSent = false;
    
    public EnhancedStreamCallbackAdapter(StreamCallback delegate, IntentType intent) {
        this.delegate = delegate;
        this.intent = intent;
    }
    
    @Override
    public void onData(String data) {
        fullResponse.append(data);
        
        // 检测JSON代码块开始
        if (data.contains("```json") || data.contains("```JSON")) {
            inJsonBlock = true;
            jsonBuffer.setLength(0);
            log.debug("🔍 检测到JSON块开始");
        }
        
        if (inJsonBlock) {
            jsonBuffer.append(data);
            
            // 检测JSON代码块结束
            if (jsonBuffer.toString().contains("```") && jsonBuffer.length() > 10) {
                String content = jsonBuffer.toString();
                int endIndex = content.lastIndexOf("```");
                if (endIndex > 7) { // 确保有实际内容
                    inJsonBlock = false;
                    String json = extractJson(content);
                    
                    if (!json.isEmpty() && !structuredDataSent) {
                        try {
                            // 发送特殊标记，前端可以识别并提取
                            delegate.onData("\n__STRUCTURED_DATA_START__\n");
                            delegate.onData(json);
                            delegate.onData("\n__STRUCTURED_DATA_END__\n");
                            structuredDataSent = true;
                            log.info("📦 已发送结构化数据，长度: {} 字符", json.length());
                        } catch (Exception e) {
                            log.error("❌ 发送结构化数据失败", e);
                        }
                    }
                }
            }
        } else {
            // 正常文本数据，直接转发给原始回调
            delegate.onData(data);
        }
    }
    
    @Override
    public void onComplete() {
        // 如果还没发送结构化数据，尝试从完整响应中提取
        if (!structuredDataSent && intent == IntentType.ITINERARY_GENERATION) {
            String json = extractJsonFromFullText(fullResponse.toString());
            if (!json.isEmpty()) {
                try {
                    delegate.onData("\n__STRUCTURED_DATA_START__\n");
                    delegate.onData(json);
                    delegate.onData("\n__STRUCTURED_DATA_END__\n");
                    structuredDataSent = true;
                    log.info("📦 从完整响应中提取并发送结构化数据，长度: {} 字符", json.length());
                } catch (Exception e) {
                    log.error("❌ 发送结构化数据失败", e);
                }
            } else {
                log.warn("⚠️ 流式响应完成，但未能提取到有效的JSON数据");
            }
        }
        
        log.info("✅ 流式响应完成 - 意图: {}, 总长度: {} 字符, 已发送结构化数据: {}", 
                intent.getDescription(), fullResponse.length(), structuredDataSent);
        delegate.onComplete();
    }
    
    @Override
    public void onError(Exception error) {
        log.error("❌ 流式响应错误 - 意图: {}", intent.getDescription(), error);
        delegate.onError(error);
    }
    
    /**
     * 从代码块中提取JSON内容
     * 
     * @param codeBlock 包含JSON的代码块
     * @return 提取出的纯JSON字符串
     */
    private String extractJson(String codeBlock) {
        try {
            // 移除代码块标记
            String cleaned = codeBlock.replaceAll("```json|```JSON|```", "").trim();
            
            // 查找第一个 { 和最后一个 }
            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");
            
            if (start >= 0 && end > start) {
                String json = cleaned.substring(start, end + 1);
                // 简单验证JSON格式（检查是否包含行程相关字段）
                if (json.contains("\"destination\"") || json.contains("\"dailyPlans\"")) {
                    log.debug("✅ 成功提取JSON，长度: {} 字符", json.length());
                    return json;
                } else {
                    log.warn("⚠️ 提取的JSON不包含必要字段");
                }
            } else {
                log.warn("⚠️ 未找到有效的JSON边界");
            }
        } catch (Exception e) {
            log.warn("⚠️ 提取JSON失败", e);
        }
        return "";
    }
    
    /**
     * 从完整文本中提取JSON（备用方法）
     * 用于当流式传输没有检测到代码块时
     * 
     * @param fullText 完整的响应文本
     * @return 提取出的纯JSON字符串
     */
    private String extractJsonFromFullText(String fullText) {
        try {
            // 方法1: 尝试从```json代码块中提取
            if (fullText.contains("```json") || fullText.contains("```JSON")) {
                int startMarker = Math.max(
                    fullText.indexOf("```json"),
                    fullText.indexOf("```JSON")
                );
                if (startMarker != -1) {
                    int endMarker = fullText.indexOf("```", startMarker + 7);
                    if (endMarker != -1) {
                        String codeBlock = fullText.substring(startMarker, endMarker + 3);
                        return extractJson(codeBlock);
                    }
                }
            }
            
            // 方法2: 直接查找JSON结构（查找包含destination和dailyPlans的最大JSON对象）
            int start = fullText.indexOf("{");
            if (start != -1) {
                // 找到最后一个包含必要字段的完整JSON对象
                int currentPos = start;
                int braceCount = 0;
                int lastValidEnd = -1;
                
                for (int i = currentPos; i < fullText.length(); i++) {
                    char c = fullText.charAt(i);
                    if (c == '{') {
                        if (braceCount == 0) currentPos = i;
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            String candidate = fullText.substring(currentPos, i + 1);
                            if (candidate.contains("\"destination\"") && 
                                candidate.contains("\"dailyPlans\"")) {
                                lastValidEnd = i;
                            }
                        }
                    }
                }
                
                if (lastValidEnd != -1) {
                    String json = fullText.substring(currentPos, lastValidEnd + 1);
                    log.debug("✅ 从完整文本中提取JSON，长度: {} 字符", json.length());
                    return json;
                }
            }
            
            log.warn("⚠️ 从完整文本中未找到有效的JSON");
        } catch (Exception e) {
            log.error("❌ 从完整文本提取JSON失败", e);
        }
        return "";
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
