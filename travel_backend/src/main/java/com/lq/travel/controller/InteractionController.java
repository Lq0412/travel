package com.lq.travel.controller;

import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.like.LikeRequest;
import com.lq.travel.model.dto.like.CommentLikeRequest;
import com.lq.travel.model.dto.post.PostQueryRequest;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.CommentLikeService;
import com.lq.travel.service.FavoriteService;
import com.lq.travel.service.LikeService;
import com.lq.travel.service.PostService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 统一交互控制器
 * 整合点赞、收藏、评论点赞功能，避免代码重复
 */
@RestController
@RequestMapping("/interaction")
@Slf4j
public class InteractionController {

    @Resource
    private LikeService likeService;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private CommentLikeService commentLikeService;

    @Resource
    private UserService userService;

    @Resource
    private PostService postService;

    // 1. 点赞帖子
    @PostMapping("/like/post")
    public ResponseDTO<Boolean> likePost(@RequestBody LikeRequest likeRequest,
                                          HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        likeService.likePost(likeRequest.getPostId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 2. 取消点赞帖子
    @PostMapping("/unlike/post")
    public ResponseDTO<Boolean> unlikePost(@RequestBody LikeRequest likeRequest,
                                           HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        likeService.unlikePost(likeRequest.getPostId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 3. 检查帖子点赞状态
    @GetMapping("/like/post/check")
    public ResponseDTO<Boolean> checkPostLike(@RequestParam Long postId,
                                              HttpServletRequest request) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        boolean isLiked = likeService.isLiked(postId, loginUser.getId());
        return ResponseUtils.success(isLiked);
    }

    // 4. 收藏帖子
    @PostMapping("/favorite/post")
    public ResponseDTO<Boolean> favoritePost(@RequestBody LikeRequest likeRequest,
                                             HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        favoriteService.addFavorite(likeRequest.getPostId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 5. 取消收藏帖子
    @PostMapping("/unfavorite/post")
    public ResponseDTO<Boolean> unfavoritePost(@RequestBody LikeRequest likeRequest,
                                               HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getPostId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        favoriteService.removeFavorite(likeRequest.getPostId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 6. 检查帖子收藏状态
    @GetMapping("/favorite/post/check")
    public ResponseDTO<Boolean> checkPostFavorite(@RequestParam Long postId,
                                                   HttpServletRequest request) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        boolean isFavorited = favoriteService.isFavorited(postId, loginUser.getId());
        return ResponseUtils.success(isFavorited);
    }

    // 7. 点赞评论
    @PostMapping("/like/comment")
    public ResponseDTO<Boolean> likeComment(@RequestBody CommentLikeRequest likeRequest,
                                             HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getCommentId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        commentLikeService.likeComment(likeRequest.getCommentId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 8. 取消点赞评论
    @PostMapping("/unlike/comment")
    public ResponseDTO<Boolean> unlikeComment(@RequestBody CommentLikeRequest likeRequest,
                                              HttpServletRequest request) {
        if (likeRequest == null || likeRequest.getCommentId() == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        commentLikeService.unlikeComment(likeRequest.getCommentId(), loginUser);
        return ResponseUtils.success(true);
    }

    // 9. 检查评论点赞状态
    @GetMapping("/like/comment/check")
    public ResponseDTO<Boolean> checkCommentLike(@RequestParam Long commentId,
                                                 HttpServletRequest request) {
        if (commentId == null || commentId <= 0) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        boolean isLiked = commentLikeService.isCommentLiked(commentId, loginUser.getId());
        return ResponseUtils.success(isLiked);
    }

    // 10. 获取最新帖子列表
    @PostMapping("/posts/latest")
    public ResponseDTO<Page<Post>> getLatestPosts(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByPage(page, "latest");
        return ResponseUtils.success(postPage);
    }

    // 11. 获取最热帖子列表（按点赞数排序）
    @PostMapping("/posts/most-liked")
    public ResponseDTO<Page<Post>> getMostLikedPosts(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByPage(page, "most_liked");
        return ResponseUtils.success(postPage);
    }

    // 12. 获取最热浏览帖子列表（按浏览数排序）
    @PostMapping("/posts/most-viewed")
    public ResponseDTO<Page<Post>> getMostViewedPosts(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByPage(page, "most_viewed");
        return ResponseUtils.success(postPage);
    }

    // 13. 获取最热评论帖子列表（按评论数排序）
    @PostMapping("/posts/most-commented")
    public ResponseDTO<Page<Post>> getMostCommentedPosts(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByPage(page, "most_commented");
        return ResponseUtils.success(postPage);
    }
}
