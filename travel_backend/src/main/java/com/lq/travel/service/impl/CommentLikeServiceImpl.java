package com.lq.travel.service.impl;

import com.lq.travel.mapper.CommentMapper;
import com.lq.travel.model.entity.Comment;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.CommentLikeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentLikeServiceImpl implements CommentLikeService {

    @Resource
    private CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId, User loginUser) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new RuntimeException("评论不存在");
        }

        // 更新点赞数
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(updateComment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId, User loginUser) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new RuntimeException("评论不存在");
        }

        // 更新点赞数
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentMapper.updateById(updateComment);
    }

    @Override
    public boolean isCommentLiked(Long commentId, Long userId) {
        // 这里可以扩展为记录用户点赞关系，类似帖子点赞
        // 目前简单实现只返回false
        return false;
    }
}