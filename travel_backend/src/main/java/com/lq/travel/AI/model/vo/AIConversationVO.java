package com.lq.travel.AI.model.vo;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话视图对象
 * 用于前端展示对话信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIConversationVO {
    
    /**
     * 对话ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 对话标题
     */
    private String title;
    
    /**
     * 使用的AI提供商
     */
    private String provider;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 对话状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 消息数量（可选，用于前端显示）
     */
    private Integer messageCount;
    
    /**
     * 最后一条消息内容（可选，用于前端显示）
     */
    private String lastMessage;
    
    /**
     * 最后一条消息时间（可选，用于前端显示）
     */
    private LocalDateTime lastMessageTime;
}
