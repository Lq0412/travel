package com.lq.travel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.post.PostQueryRequest;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.Tag;
import com.lq.travel.model.vo.PostVO;
import com.lq.travel.service.PostService;
import com.lq.travel.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统一搜索控制器
 * 整合所有搜索功能，避免代码重复
 */
@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {

    @Resource
    private PostService postService;

    @Resource
    private TagService tagService;

    // 1. 通用搜索接口
    @PostMapping("/posts")
    public ResponseDTO<Page<PostVO>> searchPosts(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new RuntimeException("参数错误");
        }

        Page<PostVO> result;
        Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());

        // 根据搜索条件选择不同的搜索方法
        if (postQueryRequest.getKeyword() != null && !postQueryRequest.getKeyword().trim().isEmpty()) {
            // 关键词搜索
            result = postService.searchPosts(page, postQueryRequest.getKeyword());
        } else if (postQueryRequest.getCategoryId() != null && postQueryRequest.getCategoryId() > 0) {
            // 分类搜索
            Page<Post> postPage = postService.listPostByCategory(page, postQueryRequest.getCategoryId());
            result = convertToPostVOPage(postPage);
        } else {
            // 默认列表
            Page<Post> postPage = postService.listPostByPage(page, postQueryRequest.getSortField());
            result = convertToPostVOPage(postPage);
        }

        return ResponseUtils.success(result);
    }

    // 2. 按标签名称搜索帖子
    @PostMapping("/posts/by-tag")
    public ResponseDTO<Page<PostVO>> searchPostsByTag(@RequestBody PostQueryRequest postQueryRequest) {
        // 参数验证
        if (postQueryRequest == null) {
            return ResponseUtils.error(40000, "请求参数不能为空");
        }
        
        String tagName = postQueryRequest.getTagName();
        if (tagName == null || tagName.trim().isEmpty()) {
            return ResponseUtils.error(40000, "标签名称不能为空");
        }

        try {
            Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
            Page<PostVO> result = postService.searchPostsByTag(page, tagName.trim());
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("按标签搜索帖子失败", e);
            return ResponseUtils.error(50000, "搜索失败: " + e.getMessage());
        }
    }

    // 3. 按标签ID搜索帖子
    @PostMapping("/posts/by-tag-id")
    public ResponseDTO<Page<PostVO>> searchPostsByTagId(@RequestBody PostQueryRequest postQueryRequest) {
        // 参数验证
        if (postQueryRequest == null) {
            return ResponseUtils.error(40000, "请求参数不能为空");
        }
        
        Long tagId = postQueryRequest.getTagId();
        if (tagId == null || tagId <= 0) {
            return ResponseUtils.error(40000, "标签ID不能为空且必须大于0");
        }

        try {
            Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
            Page<PostVO> result = postService.listPostsByTagId(page, tagId);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("按标签ID搜索帖子失败", e);
            return ResponseUtils.error(50000, "搜索失败: " + e.getMessage());
        }
    }

    // 4. 按多个标签名称搜索帖子
    @PostMapping("/posts/by-tag-names")
    public ResponseDTO<Page<PostVO>> searchPostsByTagNames(@RequestBody PostQueryRequest postQueryRequest) {
        // 参数验证
        if (postQueryRequest == null) {
            return ResponseUtils.error(40000, "请求参数不能为空");
        }
        
        List<String> tagNames = postQueryRequest.getTagNames();
        if (tagNames == null || tagNames.isEmpty()) {
            return ResponseUtils.error(40000, "标签名称列表不能为空");
        }

        try {
            Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
            Page<PostVO> result = postService.searchPostsByTagNames(page, tagNames);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("按多个标签名称搜索帖子失败", e);
            return ResponseUtils.error(50000, "搜索失败: " + e.getMessage());
        }
    }

    // 5. 按多个标签ID搜索帖子
    @PostMapping("/posts/by-tag-ids")
    public ResponseDTO<Page<PostVO>> searchPostsByTagIds(@RequestBody PostQueryRequest postQueryRequest) {
        // 参数验证
        if (postQueryRequest == null) {
            return ResponseUtils.error(40000, "请求参数不能为空");
        }
        
        List<Long> tagIds = postQueryRequest.getTagIds();
        if (tagIds == null || tagIds.isEmpty()) {
            return ResponseUtils.error(40000, "标签ID列表不能为空");
        }

        try {
            Page<Post> page = new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize());
            Page<PostVO> result = postService.searchPostsByTagIds(page, tagIds);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("按多个标签ID搜索帖子失败", e);
            return ResponseUtils.error(50000, "搜索失败: " + e.getMessage());
        }
    }

    // 6. 搜索标签
    @GetMapping("/tags")
    public ResponseDTO<List<Tag>> searchTags(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseUtils.error(40000, "搜索关键词不能为空");
        }

        try {
            List<Tag> tags = tagService.searchTags(keyword.trim());
            return ResponseUtils.success(tags);
        } catch (Exception e) {
            log.error("搜索标签失败", e);
            return ResponseUtils.error(50000, "搜索失败: " + e.getMessage());
        }
    }

    // 7. 获取热门标签
    @GetMapping("/tags/hot")
    public ResponseDTO<List<Tag>> getHotTags(@RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }

        List<Tag> hotTags = postService.getHotTags(limit);
        return ResponseUtils.success(hotTags);
    }

    // 8. 根据帖子ID获取标签
    @GetMapping("/tags/by-post/{postId}")
    public ResponseDTO<List<Tag>> getTagsByPostId(@PathVariable Long postId) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("参数错误");
        }

        List<Tag> tags = tagService.getTagsByPostId(postId);
        return ResponseUtils.success(tags);
    }

    /**
     * 将Post分页转换为PostVO分页
     */
    private Page<PostVO> convertToPostVOPage(Page<Post> postPage) {
        Page<PostVO> voPage = new Page<>();
        voPage.setCurrent(postPage.getCurrent());
        voPage.setSize(postPage.getSize());
        voPage.setTotal(postPage.getTotal());
        // voPage.setPages(postPage.getPages()); // 已弃用，使用setTotal自动计算
        
        // 转换Post列表为PostVO列表
        List<PostVO> voList = postPage.getRecords().stream()
                .map(this::convertToPostVO)
                .collect(java.util.stream.Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    /**
     * 将Post转换为PostVO
     */
    private PostVO convertToPostVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCoverUrl(post.getCoverUrl());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setCreateTime(post.getCreateTime());
        
        // 设置作者信息（这里简化处理，实际应该从用户服务获取）
        vo.setAuthorName("用户" + post.getUserId());
        vo.setAuthorAvatar("default_avatar.jpg");
        
        // 获取标签信息
        List<com.lq.travel.model.entity.Tag> tags = tagService.getTagsByPostId(post.getId());
        List<String> tagNames = tags.stream()
                .map(com.lq.travel.model.entity.Tag::getName)
                .collect(java.util.stream.Collectors.toList());
        vo.setTags(tagNames);
        
        return vo;
    }
}
