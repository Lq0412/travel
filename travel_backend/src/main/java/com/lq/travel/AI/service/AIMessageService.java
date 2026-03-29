package com.lq.travel.AI.service;

import com.lq.travel.AI.model.entity.AIMessage;
import com.lq.travel.AI.model.AIRequest;

import java.util.List;

/**
 * AI消息服务接口
 * 提供消息记录管理功能
 */
public interface AIMessageService {
    
    /**
     * 保存用户消息
     */
    AIMessage saveUserMessage(Long conversationId, String content);
    
    /**
     * 保存AI回复消息
     */
    AIMessage saveAIMessage(Long conversationId, String content, Integer tokensUsed, Long responseTime);
    
    /**
     * 获取对话的消息历史
     */
    List<AIMessage> getConversationMessages(Long conversationId);
    
    /**
     * 将消息历史转换为AIRequest格式
     */
    List<AIRequest.Message> convertToAIRequestHistory(Long conversationId);
    
    /**
     * 获取对话的最新N条消息
     */
    List<AIMessage> getRecentMessages(Long conversationId, int limit);
    
    /**
     * 删除对话的所有消息
     */
    boolean deleteConversationMessages(Long conversationId);
}
