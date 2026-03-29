package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.model.entity.AIMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI消息Mapper接口
 */
@Mapper
public interface AIMessageMapper extends BaseMapper<AIMessage> {
    
    /**
     * 获取对话的消息列表（按时间排序）
     */
    List<AIMessage> selectConversationMessages(@Param("conversationId") Long conversationId);
    
    /**
     * 获取对话的最新N条消息
     */
    List<AIMessage> selectRecentMessages(@Param("conversationId") Long conversationId, 
                                        @Param("limit") int limit);
    
    /**
     * 删除对话的所有消息
     */
    int deleteConversationMessages(@Param("conversationId") Long conversationId);
}
