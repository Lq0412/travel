package com.lq.travel.AI.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.AI.model.entity.AIConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话Mapper接口
 */
@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {
    
    /**
     * 获取用户的对话列表（分页）
     */
    List<AIConversation> selectUserConversations(@Param("userId") Long userId, 
                                                @Param("offset") int offset, 
                                                @Param("limit") int limit);
    
    /**
     * 统计用户的对话数量
     */
    int countUserConversations(@Param("userId") Long userId);
    
    /**
     * 软删除对话
     */
    int softDeleteConversation(@Param("conversationId") Long conversationId);
}
