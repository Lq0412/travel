package com.lq.travel.callback;

/**
 * 流式响应回调接口
 */
public interface StreamCallback {
    
    /**
     * 接收到新的数据块
     */
    void onData(String data);
    
    /**
     * 流式响应完成
     */
    void onComplete();
    
    /**
     * 发生错误
     */
    void onError(Exception error);
}
