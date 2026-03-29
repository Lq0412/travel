package com.lq.travel.AI.core.callback;

import com.lq.travel.constant.RegexPatterns;
import com.lq.travel.AI.core.interfaces.StreamCallback;

import java.util.regex.Matcher;

/**
 * 清理文本的流式回调适配器
 * 在StreamCallbackAdapter的基础上增加文本清理功能
 * 
 * <p>功能：
 * <ul>
 *   <li>去除结构化标签（思考、行动、观察等）</li>
 *   <li>去除emoji前缀</li>
 *   <li>去除身份标识</li>
 *   <li>去除多余空格</li>
 *   <li>按句子边界分段输出</li>
 * </ul>
 * 
 * <p>适用场景：
 * <ul>
 *   <li>数字人对话</li>
 *   <li>语音播报</li>
 *   <li>需要纯净文本的场景</li>
 * </ul>
 */
public class CleaningStreamCallbackAdapter implements StreamCallback {
    
    /** 委托的回调对象 */
    private final StreamCallback delegate;
    
    /** 数据缓冲区 */
    private final StringBuilder buffer = new StringBuilder();
    
    /**
     * 构造函数
     * 
     * @param delegate 实际处理数据的回调对象
     */
    public CleaningStreamCallbackAdapter(StreamCallback delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate不能为null");
        }
        this.delegate = delegate;
    }
    
    @Override
    public void onData(String data) {
        try {
            // 清理文本
            String cleanedData = cleanText(data);
            if (cleanedData.isEmpty()) {
                return;
            }
            
            // 将清理后的数据添加到缓冲区
            buffer.append(cleanedData);
            
            // 提取完整的句子
            String chunk = extractCompleteSentences();
            if (!chunk.isEmpty()) {
                delegate.onData(chunk);
            }
        } catch (Exception e) {
            delegate.onError(e);
        }
    }
    
    @Override
    public void onComplete() {
        try {
            // 输出缓冲区中剩余的数据
            if (buffer.length() > 0) {
                String remaining = cleanText(buffer.toString());
                if (!remaining.isEmpty()) {
                    delegate.onData(remaining);
                }
                buffer.setLength(0);
            }
            delegate.onComplete();
        } catch (Exception e) {
            delegate.onError(e);
        }
    }
    
    @Override
    public void onError(Exception e) {
        delegate.onError(e);
    }
    
    /**
     * 清理文本，去除结构化标签和不需要的符号
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = text;
        
        // 去除结构化标签（思考、行动、观察、推理）
        cleaned = RegexPatterns.CLEAN_TEXT.matcher(cleaned).replaceAll("");
        
        // 去除单独的emoji前缀
        cleaned = RegexPatterns.EMOJI_PREFIX.matcher(cleaned).replaceAll("");
        
        // 去除身份标识
        cleaned = RegexPatterns.IDENTITY_MARK.matcher(cleaned).replaceAll("");
        
        // 去除多余的空格和换行
        cleaned = RegexPatterns.MULTIPLE_SPACES.matcher(cleaned).replaceAll(" ").trim();
        
        return cleaned;
    }
    
    /**
     * 从缓冲区中提取完整的句子
     * 
     * @return 完整的句子，如果没有则返回空字符串
     */
    private String extractCompleteSentences() {
        Matcher m = RegexPatterns.SENTENCE_BOUNDARY.matcher(buffer);
        
        // 找到最后一个句子边界的位置
        int lastBoundary = -1;
        while (m.find()) {
            lastBoundary = m.end();
        }
        
        // 如果找到了句子边界
        if (lastBoundary > 0) {
            String chunk = buffer.substring(0, lastBoundary);
            buffer.delete(0, lastBoundary);
            return chunk;
        }
        
        return "";
    }
    
    /**
     * 清空缓冲区
     * 用于手动重置适配器状态
     */
    public void clearBuffer() {
        buffer.setLength(0);
    }
    
    /**
     * 获取当前缓冲区的内容
     * 
     * @return 缓冲区内容
     */
    public String getBufferedContent() {
        return buffer.toString();
    }
}


