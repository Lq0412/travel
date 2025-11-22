package com.lq.travel.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景点留言墙配置VO
 */
@Data
public class ScenicMessageWallVO {
    
    private Long scenicSpotId;
    
    private String scenicSpotName;
    
    private String title;
    
    private String description;
    
    private Long messageCount;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

