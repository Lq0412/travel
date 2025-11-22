package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.comment.CommentQueryRequest;
import com.lq.travel.model.entity.Comment;
import com.lq.travel.model.entity.User;

public interface CommentService extends IService<Comment> {
    void addComment(Comment comment, User loginUser);
    void deleteComment(Long commentId, User loginUser);
    void updateComment(Comment comment, User loginUser);
    Page<Comment> listCommentsByPage(Long postId, Page<Comment> page);
    void addReply(Comment comment, User loginUser);
    Page<Comment> listRepliesByPage(Long commentId, Page<Comment> page);
    Page<Comment> listCommentsByPage(CommentQueryRequest queryRequest);
    Page<Comment> listRepliesByPage(CommentQueryRequest queryRequest);
}