package com.lq.travel.model.dto.comment;

import lombok.Data;

@Data
public class CommentQueryRequest {
    private Long postId;
    private Integer current = 1;
    private Integer pageSize = 10;
    private Integer commentId;
}