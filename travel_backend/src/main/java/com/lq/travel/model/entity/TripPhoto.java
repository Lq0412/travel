package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行程照片实体
 */
@Data
@TableName("trip_photo")
public class TripPhoto implements Serializable {
    
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
     * 照片URL
     */
    private String photoUrl;
    
    /**
     * 拍摄时间（可选）
     */
    private Date shotTime;
    
    /**
     * 照片描述（可选）
     */
    private String description;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
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

