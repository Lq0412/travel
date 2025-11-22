package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 标签对应关系
 */
@Data
@TableName("post_tag")
public class PostTag {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}