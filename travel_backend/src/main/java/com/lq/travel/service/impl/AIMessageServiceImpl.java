package com.lq.travel.service.impl;

import com.lq.travel.mapper.AIMessageMapper;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI消息服务实现类
 */
@Slf4j
@Service
public class AIMessageServiceImpl implements AIMessageService {
    
    @Resource
    private AIMessageMapper messageMapper;
    
    @Override
    public AIMessage saveUserMessage(Long conversationId, String content) {
        AIMessage message = AIMessage.builder()
                .conversationId(conversationId)
                .role("user")
                .content(content)
                .createTime(LocalDateTime.now())
                .isDelete(false)
                .build();
        
        messageMapper.insert(message);
        log.debug("保存用户消息: conversationId={}, messageId={}", conversationId, message.getId());
        return message;
    }
    
    @Override
    public AIMessage saveAIMessage(Long conversationId, String content, Integer tokensUsed, Long responseTime) {
        AIMessage message = AIMessage.builder()
                .conversationId(conversationId)
                .role("assistant")
                .content(content)
                .tokensUsed(tokensUsed)
                .responseTime(responseTime)
                .createTime(LocalDateTime.now())
                .isDelete(false)
                .build();
        
        messageMapper.insert(message);
        log.debug("保存AI消息: conversationId={}, messageId={}", conversationId, message.getId());
        return message;
    }
    
    @Override
    public List<AIMessage> getConversationMessages(Long conversationId) {
        return messageMapper.selectConversationMessages(conversationId);
    }
    
    @Override
    public List<AIRequest.Message> convertToAIRequestHistory(Long conversationId) {
        List<AIMessage> messages = getConversationMessages(conversationId);
        
        return messages.stream()
                .map(this::convertToAIRequestMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AIMessage> getRecentMessages(Long conversationId, int limit) {
        return messageMapper.selectRecentMessages(conversationId, limit);
    }
    
    @Override
    public boolean deleteConversationMessages(Long conversationId) {
        int result = messageMapper.deleteConversationMessages(conversationId);
        log.info("删除对话消息: conversationId={}, 删除数量={}", conversationId, result);
        return result > 0;
    }
    
    /**
     * 转换为AIRequest.Message格式
     */
    private AIRequest.Message convertToAIRequestMessage(AIMessage message) {
        return AIRequest.Message.builder()
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getCreateTime() != null ? 
                    message.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                    System.currentTimeMillis())
                .build();
    }
}
