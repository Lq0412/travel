package com.lq.travel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 行程视图对象
 */
@Data
public class TripVO implements Serializable {
    
    private Long id;
    private Long userId;
    private String destination;
    private Integer days;
    private Integer budget;
    private String theme;
    private Date startDate;
    private Date endDate;
    private Map<Integer, List<String>> dailyHighlights;
    private String status;
    private Date createTime;
    private Date updateTime;
    
    /**
     * 照片列表
     */
    private List<TripPhotoVO> photos;
    
    /**
     * 回忆图信息
     */
    private MemoryCardVO memoryCard;

    /**
     * 是否已发布到灵感广场
     */
    private Boolean publishedToInspiration;

    /**
     * 已发布内容ID
     */
    private Long publishedPostId;
    
    private static final long serialVersionUID = 1L;
}

