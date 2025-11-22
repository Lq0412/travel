package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 回忆图实体
 */
@Data
@TableName("memory_card")
public class MemoryCard implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 行程ID
     */
    private Long tripId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 模板名称（固定模板，MVP阶段只有一个）
     */
    private String templateName;
    
    /**
     * 生成结果URL
     */
    private String imageUrl;
    
    /**
     * 生成任务ID（用于异步任务轮询）
     */
    private String taskId;
    
    /**
     * 生成状态：pending（待生成）、processing（生成中）、success（成功）、failed（失败）
     */
    private String status;
    
    /**
     * 错误信息（如果生成失败）
     */
    private String errorMessage;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    
    private static final long serialVersionUID = 1L;
}

