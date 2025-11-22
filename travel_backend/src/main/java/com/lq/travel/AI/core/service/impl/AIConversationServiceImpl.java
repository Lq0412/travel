package com.lq.travel.AI.core.service.impl;

import com.lq.travel.AI.core.mapper.AIConversationMapper;
import com.lq.travel.AI.core.model.entity.AIConversation;
import com.lq.travel.AI.core.model.vo.AIConversationVO;
import com.lq.travel.AI.core.service.AIConversationService;
import com.lq.travel.AI.core.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI对话服务实现类
 */
@Slf4j
@Service
public class AIConversationServiceImpl implements AIConversationService {
    
    @Resource
    private AIConversationMapper conversationMapper;
    
    @Resource
    private AIMessageService messageService;
    
    @Override
    public AIConversation createConversation(Long userId, String title, String provider, String model) {
        AIConversation conversation = AIConversation.builder()
                .userId(userId)
                .title(title)
                .provider(provider)
                .model(model)
                .status("active")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(false)
                .build();
        
        conversationMapper.insert(conversation);
        log.info("创建新对话: userId={}, conversationId={}", userId, conversation.getId());
        return conversation;
    }
    
    @Override
    public AIConversation getConversationById(Long conversationId) {
        // MyBatis Plus的@TableLogic注解已经自动过滤了已删除的记录
        // 所以这里直接返回查询结果即可
        return conversationMapper.selectById(conversationId);
    }
    
    @Override
    public List<AIConversationVO> getUserConversations(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<AIConversation> conversations = conversationMapper.selectUserConversations(userId, offset, pageSize);
        
        return conversations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean updateConversationTitle(Long conversationId, String title) {
        AIConversation conversation = new AIConversation();
        conversation.setId(conversationId);
        conversation.setTitle(title);
        conversation.setUpdateTime(LocalDateTime.now());
        
        int result = conversationMapper.updateById(conversation);
        return result > 0;
    }
    
    @Override
    public boolean deleteConversation(Long conversationId) {
        try {
            // 使用专门的软删除方法
            int result = conversationMapper.softDeleteConversation(conversationId);
            log.info("软删除对话: conversationId={}, result={}", conversationId, result);
            
            if (result > 0) {
                log.info("对话删除成功: conversationId={}", conversationId);
                return true;
            } else {
                log.warn("对话删除失败或对话不存在/已删除: conversationId={}", conversationId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("删除对话失败: conversationId={}", conversationId, e);
            return false;
        }
    }
    
    @Override
    public boolean deleteConversationWithMessages(Long conversationId) {
        try {
            // 1. 先检查对话是否存在（包括已删除的）
            AIConversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.warn("尝试删除不存在的对话: {}", conversationId);
                return false;
            }
            
            // 2. 检查对话是否已经被删除
            if (Boolean.TRUE.equals(conversation.getIsDelete())) {
                log.warn("对话已经被删除: {}", conversationId);
                return true; // 认为删除成功，因为目标状态已达成
            }
            
            // 3. 先删除所有相关消息
            boolean messagesDeleted = messageService.deleteConversationMessages(conversationId);
            log.info("删除对话消息结果: conversationId={}, success={}", conversationId, messagesDeleted);
            
            // 4. 再删除对话记录
            boolean conversationDeleted = deleteConversation(conversationId);
            log.info("删除对话记录结果: conversationId={}, success={}", conversationId, conversationDeleted);
            
            return conversationDeleted;
            
        } catch (Exception e) {
            log.error("删除对话及其消息失败: conversationId={}", conversationId, e);
            return false;
        }
    }
    
    @Override
    public boolean isConversationOwner(Long conversationId, Long userId) {
        AIConversation conversation = getConversationById(conversationId);
        if (conversation == null) {
            log.warn("会话不存在: conversationId={}, userId={}", conversationId, userId);
            return false;
        }
        
        Long conversationUserId = conversation.getUserId();
        boolean isOwner = conversationUserId != null && conversationUserId.equals(userId);
        
        if (!isOwner) {
            log.warn("用户无权访问该会话: conversationId={}, conversationUserId={}, requestUserId={}", 
                    conversationId, conversationUserId, userId);
        }
        
        return isOwner;
    }
    
    /**
     * 转换为VO对象
     */
    private AIConversationVO convertToVO(AIConversation conversation) {
        AIConversationVO vo = new AIConversationVO();
        BeanUtils.copyProperties(conversation, vo);
        return vo;
    }
}
