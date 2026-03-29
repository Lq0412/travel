package com.lq.travel.AI.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代理任务实体
 * 用于存储代理执行的任务记录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentTask {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 代理名称
     */
    private String agentName;
    
    /**
     * 任务描述
     */
    private String task;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 执行时间(毫秒)
     */
    private Long executionTime;
    
    /**
     * 执行步数
     */
    private Integer stepCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否删除
     */
    private Boolean isDelete;
}
