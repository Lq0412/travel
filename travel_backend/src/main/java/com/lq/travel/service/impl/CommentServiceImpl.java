package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.CommentMapper;
import com.lq.travel.mapper.PostMapper;
import com.lq.travel.model.dto.comment.CommentQueryRequest;
import com.lq.travel.model.entity.Comment;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(Comment comment, User loginUser) {
        if (comment == null || comment.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        // 校验帖子是否存在
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null || post.getIsDelete() == 1) {
            throw new RuntimeException("帖子不存在");
        }

        // 设置用户ID
        comment.setUserId(loginUser.getId());

        // 保存评论
        boolean saved = this.save(comment);
        if (!saved) {
            throw new RuntimeException("评论失败");
        }

        // 更新帖子评论数
        Post updatePost = new Post();
        updatePost.setId(post.getId());
        updatePost.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(updatePost);
    }

    @Override
    public Page<Comment> listCommentsByPage(Long postId, Page<Comment> page) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("参数错误");
        }

        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");

        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, User loginUser) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 检查权限
        if (!comment.getUserId().equals(loginUser.getId())) {
            throw new RuntimeException("无权删除此评论");
        }

        // 逻辑删除
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setIsDelete(1);
        this.updateById(updateComment);

        // 更新帖子评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getIsDelete() == 0) {
            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postMapper.updateById(updatePost);
        }
    }

    @Override
    public void updateComment(Comment comment, User loginUser) {
        if (comment == null || comment.getId() == null) {
            throw new RuntimeException("参数错误");
        }

        Comment oldComment = this.getById(comment.getId());
        if (oldComment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 检查权限
        if (!oldComment.getUserId().equals(loginUser.getId())) {
            throw new RuntimeException("无权修改此评论");
        }

        // 只允许更新内容
        Comment updateComment = new Comment();
        updateComment.setId(comment.getId());
        updateComment.setContent(comment.getContent());
        this.updateById(updateComment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReply(Comment comment, User loginUser) {
        if (comment == null || comment.getPostId() == null || comment.getParentId() == null) {
            throw new RuntimeException("参数错误");
        }

        // 校验父评论是否存在
        Comment parentComment = this.getById(comment.getParentId());
        if (parentComment == null || parentComment.getIsDelete() == 1) {
            throw new RuntimeException("父评论不存在");
        }

        // 设置用户ID
        comment.setUserId(loginUser.getId());

        // 保存评论
        boolean saved = this.save(comment);
        if (!saved) {
            throw new RuntimeException("回复失败");
        }

        // 更新帖子评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getIsDelete() == 0) {
            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(updatePost);
        }
    }

    @Override
    public Page<Comment> listRepliesByPage(Long commentId, Page<Comment> page) {
        if (commentId == null || commentId <= 0) {
            throw new RuntimeException("参数错误");
        }

        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", commentId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");

        return this.page(page, queryWrapper);
    }

    @Override
    public Page<Comment> listCommentsByPage(CommentQueryRequest queryRequest) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", queryRequest.getPostId());
        queryWrapper.eq("parent_id", 0); // 只查顶级评论
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");

        return this.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                queryWrapper);
    }

    @Override
    public Page<Comment> listRepliesByPage(CommentQueryRequest queryRequest) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", queryRequest.getCommentId());
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByAsc("create_time"); // 回复按时间正序排列

        return this.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                queryWrapper);
    }

}