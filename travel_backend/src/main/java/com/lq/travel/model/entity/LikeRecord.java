package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 点赞
 */
@Data
@TableName("like_record")
public class LikeRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long postId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private static final long serialVersionUID = 1L;
}