package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论
 */
@Data
@TableName("comment")
public class Comment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long postId;
    private String content;
    private Integer likeCount = 0;
    private Long parentId = 0L; // 0表示顶级评论
    private Long replyToUserId; // 被回复的用户ID

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}