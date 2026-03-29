package com.lq.travel.AI.callback;

import com.lq.travel.constant.RegexPatterns;
import com.lq.travel.AI.interfaces.StreamCallback;

import java.util.regex.Matcher;

/**
 * 流式回调适配器
 * 负责处理流式数据的句子边界检测和分段输出
 * 
 * <p>功能：
 * <ul>
 *   <li>缓冲流式数据</li>
 *   <li>检测句子边界（句号、感叹号、问号、换行）</li>
 *   <li>按完整句子分段输出</li>
 *   <li>确保最后的数据也能输出</li>
 * </ul>
 */
public class StreamCallbackAdapter implements StreamCallback {
    
    /** 委托的回调对象 */
    private final StreamCallback delegate;
    
    /** 数据缓冲区 */
    private final StringBuilder buffer = new StringBuilder();
    
    /**
     * 构造函数
     * 
     * @param delegate 实际处理数据的回调对象
     */
    public StreamCallbackAdapter(StreamCallback delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate不能为null");
        }
        this.delegate = delegate;
    }
    
    @Override
    public void onData(String data) {
        try {
            // 将数据添加到缓冲区
            buffer.append(data);
            
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
                delegate.onData(buffer.toString());
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


