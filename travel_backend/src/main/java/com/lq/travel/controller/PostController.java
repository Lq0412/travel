package com.lq.travel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.post.PostAddRequest;
import com.lq.travel.model.dto.post.PostQueryRequest;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.Tag;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.PostVO;
import com.lq.travel.service.CategoryService;
import com.lq.travel.service.PostService;
import com.lq.travel.service.TagService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private TagService tagService;

    // 1. 发布帖子
    @PostMapping("/add")
    public ResponseDTO<Long> addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        if (postAddRequest == null) {
            throw new RuntimeException("参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Post post = new Post();
        post.setTitle(postAddRequest.getTitle());
        post.setContent(postAddRequest.getContent());
        post.setCoverUrl(postAddRequest.getCoverUrl());
        post.setUserId(loginUser.getId());
        post.setCategoryId(postAddRequest.getCategoryId());
        
        // 设置默认值
        post.setStatus(1); // 1-正常状态
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        
        // 手动设置时间
        Date now = new Date();
        post.setCreateTime(now);
        post.setUpdateTime(now);

        // 使用新的addPost方法，同时处理标签
        Long postId = postService.addPost(post, postAddRequest.getTags());

        return ResponseUtils.success(postId);
    }

    // 2. 帖子列表
    @PostMapping("/list/page")
    public ResponseDTO<Page<Post>> listPostByPage(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByPage(page, postQueryRequest.getSortField());

        return ResponseUtils.success(postPage);
    }

    // 3. 帖子详情
    @GetMapping("/get/{id}")
    public ResponseDTO<PostVO> getPostById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }

        PostVO postVO = postService.getPostDetailById(id, true);
        return ResponseUtils.success(postVO);
    }

    // 4. 根据分类获取帖子列表
    @PostMapping("/list/byCategory")
    public ResponseDTO<Page<Post>> listPostByCategory(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
        Page<Post> postPage = postService.listPostByCategory(page, postQueryRequest.getCategoryId());

        return ResponseUtils.success(postPage);
    }



    // 5. 更新帖子标签
    @PostMapping("/{postId}/tags")
    public ResponseDTO<Boolean> updatePostTags(@PathVariable Long postId, @RequestBody List<String> tags) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("参数错误");
        }

        postService.updatePostTags(postId, tags);
        return ResponseUtils.success(true);
    }

    // 6. 获取所有标签
    @GetMapping("/tags/all")
    public ResponseDTO<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.list();
        return ResponseUtils.success(tags);
    }
}