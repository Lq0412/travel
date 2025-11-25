package com.lq.travel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 回忆图视图对象
 */
@Data
public class MemoryCardVO implements Serializable {
    
    private Long id;
    private Long tripId;
    private Long userId;
    private String templateName;
    private String imageUrl;
    private String taskId;
    private String remoteTaskId;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Date createTime;
    private Date updateTime;
    
    private static final long serialVersionUID = 1L;
}

