package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 回忆图历史版本实体
 */
@Data
@TableName("memory_card_history")
public class MemoryCardHistory implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tripId;
    private Long userId;
    private String templateName;
    private String imageUrl;
    private String taskId;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Date createTime;
    private Date updateTime;
    
    private static final long serialVersionUID = 1L;
}

