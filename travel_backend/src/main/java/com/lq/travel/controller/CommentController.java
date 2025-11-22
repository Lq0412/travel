package com.lq.travel.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.comment.CommentAddRequest;
import com.lq.travel.model.dto.comment.CommentQueryRequest;
import com.lq.travel.model.entity.Comment;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.CommentService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {

    @Resource
    private CommentService commentService;

    @Resource
    private UserService userService;

    // 1. 评论帖子
    @PostMapping("/add")
    public ResponseDTO<Boolean> addComment(@RequestBody CommentAddRequest commentAddRequest,
                                            HttpServletRequest request) {
        if (commentAddRequest == null || commentAddRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);

        Comment comment = new Comment();
        comment.setPostId(commentAddRequest.getPostId());
        comment.setContent(commentAddRequest.getContent());

        commentService.addComment(comment, loginUser);
        return ResponseUtils.success(true);
    }

    // 2. 分页获取评论列表
    @PostMapping("/list/page")
    public ResponseDTO<Page<Comment>> listCommentByPage(@RequestBody CommentQueryRequest commentQueryRequest) {
        if (commentQueryRequest == null || commentQueryRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Comment> page = new Page<>(commentQueryRequest.getCurrent(), commentQueryRequest.getPageSize());
        Page<Comment> commentPage = commentService.listCommentsByPage(commentQueryRequest.getPostId(), page);

        return ResponseUtils.success(commentPage);
    }

    // 3. 删除评论
    @PostMapping("/delete")
    public ResponseDTO<Boolean> deleteComment(@RequestParam Long commentId,
                                               HttpServletRequest request) {
        if (commentId == null || commentId <= 0) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        commentService.deleteComment(commentId, loginUser);
        return ResponseUtils.success(true);
    }

    // 4. 更新评论
    @PostMapping("/update")
    public ResponseDTO<Boolean> updateComment(@RequestBody Comment comment,
                                               HttpServletRequest request) {
        if (comment == null || comment.getId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        commentService.updateComment(comment, loginUser);
        return ResponseUtils.success(true);
    }

    // 5. 添加回复
    @PostMapping("/reply/add")
    public ResponseDTO<Boolean> addReply(@RequestBody CommentAddRequest commentAddRequest,
                                          HttpServletRequest request) {
        if (commentAddRequest == null || commentAddRequest.getPostId() == null
                || commentAddRequest.getParentId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);

        Comment comment = new Comment();
        comment.setPostId(commentAddRequest.getPostId());
        comment.setParentId(commentAddRequest.getParentId());
        comment.setReplyToUserId(commentAddRequest.getReplyToUserId());
        comment.setContent(commentAddRequest.getContent());

        commentService.addReply(comment, loginUser);
        return ResponseUtils.success(true);
    }

    // 6. 分页获取回复列表
    @PostMapping("/reply/list/page")
    public ResponseDTO<Page<Comment>> listRepliesByPage(@RequestBody CommentQueryRequest commentQueryRequest) {
        if (commentQueryRequest == null || commentQueryRequest.getCommentId() == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Comment> page = new Page<>(commentQueryRequest.getCurrent(), commentQueryRequest.getPageSize());
        Page<Comment> repliesPage = commentService.listRepliesByPage(Long.valueOf(commentQueryRequest.getCommentId()), page);

        return ResponseUtils.success(repliesPage);
    }
}