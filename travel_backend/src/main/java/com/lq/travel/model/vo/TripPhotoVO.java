package com.lq.travel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行程照片视图对象
 */
@Data
public class TripPhotoVO implements Serializable {
    
    private Long id;
    private Long tripId;
    private String photoUrl;
    private Date shotTime;
    private String description;
    private Integer sortOrder;
    private Date createTime;
    
    private static final long serialVersionUID = 1L;
}

