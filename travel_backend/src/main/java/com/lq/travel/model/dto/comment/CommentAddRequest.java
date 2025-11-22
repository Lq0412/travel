package com.lq.travel.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentAddRequest implements Serializable {
    private Long postId;
    private String content;
    private Long parentId = 0L; // 父评论ID（默认0表示顶级评论）
    private Long replyToUserId; // 被回复的用户ID（可选）
}