package com.lq.travel.AI.service;

import com.lq.travel.model.entity.AIConversation;
import com.lq.travel.model.vo.AIConversationVO;

import java.util.List;

/**
 * AI对话服务接口
 * 提供对话会话管理功能
 */
public interface AIConversationService {
    
    /**
     * 创建新对话
     */
    AIConversation createConversation(Long userId, String title, String provider, String model);
    
    /**
     * 根据ID获取对话
     */
    AIConversation getConversationById(Long conversationId);
    
    /**
     * 获取用户的对话列表
     */
    List<AIConversationVO> getUserConversations(Long userId, int pageNum, int pageSize);
    
    /**
     * 更新对话标题
     */
    boolean updateConversationTitle(Long conversationId, String title);
    
    /**
     * 删除对话
     */
    boolean deleteConversation(Long conversationId);
    
    /**
     * 删除对话及其所有相关消息
     */
    boolean deleteConversationWithMessages(Long conversationId);
    
    /**
     * 检查对话是否属于用户
     */
    boolean isConversationOwner(Long conversationId, Long userId);
}
