package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 收藏
 */
@Data
@TableName("favorite")
public class Favorite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer type = 1; // 1-帖子
    private Long targetId; // 帖子ID

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private static final long serialVersionUID = 1L;
}