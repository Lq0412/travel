package com.lq.travel.AI.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI消息实体
 * 用于存储AI对话中的消息记录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_message")
public class AIMessage {
    
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 对话ID
     */
    private Long conversationId;
    
    /**
     * 消息角色：user, assistant, system
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 使用的token数
     */
    private Integer tokensUsed;
    
    /**
     * 响应时间(毫秒)
     */
    private Long responseTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 是否删除
     */
    private Boolean isDelete;
}
