package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.PostMapper;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.Tag;
import com.lq.travel.model.vo.PostVO;
import com.lq.travel.service.PostService;
import com.lq.travel.service.PostTagService;
import com.lq.travel.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private TagService tagService;

    @Resource
    private PostTagService postTagService;


    @Override
    public void validPost(Post post, boolean add) {
        if (post == null) {
            throw new RuntimeException("帖子不能为空");
        }

        String title = post.getTitle();
        String content = post.getContent();

        if (StringUtils.isBlank(title)) {
            throw new RuntimeException("标题不能为空");
        }
        if (title.length() > 255) {
            throw new RuntimeException("标题过长");
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("内容不能为空");
        }
    }

    //热度排序
    @Override
    public Page<Post> listPostByPage(Page<Post> page, String sortField) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);

        // 根据排序字段进行排序
        if ("latest".equals(sortField) || "create_time".equals(sortField)) {
            // 最新发布：按创建时间降序
            queryWrapper.orderByDesc("create_time");
        } else if ("most_liked".equals(sortField) || "like_count".equals(sortField)) {
            // 最多点赞：按点赞数降序
            queryWrapper.orderByDesc("like_count", "create_time");
        } else if ("most_viewed".equals(sortField) || "view_count".equals(sortField)) {
            // 最多浏览：按浏览数降序
            queryWrapper.orderByDesc("view_count", "create_time");
        } else if ("most_commented".equals(sortField) || "comment_count".equals(sortField)) {
            // 最多评论：按评论数降序
            queryWrapper.orderByDesc("comment_count", "create_time");
        } else if ("hot".equals(sortField)) {
            // 热度排序：综合点赞、评论、浏览数
            queryWrapper.orderByDesc("like_count", "comment_count", "view_count", "create_time");
        } else {
            // 默认：按创建时间降序
            queryWrapper.orderByDesc("create_time");
        }

        return this.page(page, queryWrapper);
    }

    //浏览逻辑+1
    @Override
    public Post getPostById(Long id, boolean incrementView) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }

        Post post = this.getById(id);
        if (post == null || post.getIsDelete() == 1) {
            throw new RuntimeException("帖子不存在");
        }

        if (incrementView) {
            baseMapper.incrementViewCount(id);
            post.setViewCount(post.getViewCount() + 1);
        }

        return post;
    }

    //分类
    @Override
    public Page<Post> listPostByCategory(Page<Post> page, Integer categoryId) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq("category_id", categoryId);
        }
        queryWrapper.orderByDesc("create_time");

        return this.page(page, queryWrapper);
    }

    //从标题搜索增加了内容搜索
    @Override
    public Page<PostVO> searchPosts(Page<Post> page, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            throw new RuntimeException("搜索关键词不能为空");
        }

        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.and(wrapper -> wrapper.like("title", keyword).or().like("content", keyword));
        queryWrapper.orderByDesc("create_time");

        Page<Post> postPage = this.page(page, queryWrapper);

        // 转换为VO
        Page<PostVO> voPage = new Page<>();
        voPage.setCurrent(postPage.getCurrent());
        voPage.setSize(postPage.getSize());
        voPage.setTotal(postPage.getTotal());
        // voPage.setPages(postPage.getPages()); // 已弃用，使用setTotal自动计算

        List<PostVO> voList = postPage.getRecords().stream().map(this::convertToPostVO).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public Long addPost(Post post, List<String> tags) {
        // 验证帖子
        validPost(post, true);

        // 保存帖子
        this.save(post);

        // 处理标签
        if (tags != null && !tags.isEmpty()) {
            tagService.updatePostTags(post.getId(), tags);
        }

        return post.getId();
    }

    @Override
    public Page<PostVO> searchPostsByTag(Page<Post> page, String tagName) {
        if (StringUtils.isBlank(tagName)) {
            throw new RuntimeException("标签名称不能为空");
        }

        // 查找标签
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
        tagQueryWrapper.eq("name", tagName);
        tagQueryWrapper.eq("is_delete", 0);
        Tag tag = tagService.getOne(tagQueryWrapper);

        if (tag == null) {
            // 如果没有找到标签，返回空分页
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 获取关联的帖子ID
        List<Long> postIds = postTagService.getPostIdsByTagId(tag.getId());

        if (postIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 查询帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", postIds);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");

        Page<Post> postPage = this.page(page, queryWrapper);

        // 转换为VO
        return convertToPostVOPage(postPage);
    }

    @Override
    public PostVO getPostDetailById(Long id, boolean incrementView) {
        Post post = getPostById(id, incrementView);
        PostVO postVO = convertToPostVO(post);
        return postVO;
    }

    @Override
    @Transactional
    public void updatePostTags(Long postId, List<String> tags) {
        tagService.updatePostTags(postId, tags);
    }

    @Override
    public Page<PostVO> listPostsByTagId(Page<Post> page, Long tagId) {
        if (tagId == null || tagId <= 0) {
            throw new RuntimeException("标签ID不能为空");
        }

        // 获取关联的帖子ID
        List<Long> postIds = postTagService.getPostIdsByTagId(tagId);

        if (postIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 查询帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", postIds);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");

        Page<Post> postPage = this.page(page, queryWrapper);

        // 转换为VO
        return convertToPostVOPage(postPage);
    }

    @Override
    public Page<PostVO> searchPostsByTagNames(Page<Post> page, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            throw new RuntimeException("标签名称列表不能为空");
        }

        // 查找所有匹配的标签
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
        tagQueryWrapper.in("name", tagNames);
        tagQueryWrapper.eq("is_delete", 0);
        List<Tag> tags = tagService.list(tagQueryWrapper);

        if (tags.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 获取所有标签的ID
        List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());

        // 获取关联的帖子ID（包含任一标签的帖子）
        List<Long> postIds = postTagService.getPostIdsByTagIds(tagIds);

        if (postIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 查询帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", postIds);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");

        Page<Post> postPage = this.page(page, queryWrapper);

        // 转换为VO
        return convertToPostVOPage(postPage);
    }

    @Override
    public Page<PostVO> searchPostsByTagIds(Page<Post> page, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new RuntimeException("标签ID列表不能为空");
        }

        // 获取关联的帖子ID（包含任一标签的帖子）
        List<Long> postIds = postTagService.getPostIdsByTagIds(tagIds);

        if (postIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 查询帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", postIds);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");

        Page<Post> postPage = this.page(page, queryWrapper);

        // 转换为VO
        return convertToPostVOPage(postPage);
    }

    @Override
    public List<Tag> getHotTags(int limit) {
        // 这里可以使用自定义SQL查询热门标签
        // 简化实现：返回所有标签
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT " + limit);

        return tagService.list(queryWrapper);
    }

    /**
     * 将Post转换为PostVO（包含标签信息）
     */
    private PostVO convertToPostVO(Post post) {
        PostVO vo = new PostVO();
        // 复制基本属性
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCoverUrl(post.getCoverUrl());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setCreateTime(post.getCreateTime());

        // 设置作者信息
        vo.setAuthorName("用户" + post.getUserId()); // 实际应从用户服务获取
        vo.setAuthorAvatar("default_avatar.jpg"); // 实际应从用户服务获取

        // 设置标签信息
        List<Tag> tags = tagService.getTagsByPostId(post.getId());
        List<String> tagNames = tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        vo.setTags(tagNames);

        return vo;
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

        List<PostVO> voList = postPage.getRecords().stream()
                .map(this::convertToPostVO)
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}