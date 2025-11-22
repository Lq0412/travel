package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言点赞表
 */
@Data
@TableName("message_like")
public class MessageLike {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long messageId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}