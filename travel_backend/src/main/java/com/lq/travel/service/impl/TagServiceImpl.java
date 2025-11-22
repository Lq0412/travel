package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.PostTagMapper;
import com.lq.travel.mapper.TagMapper;
import com.lq.travel.model.entity.PostTag;
import com.lq.travel.model.entity.Tag;
import com.lq.travel.service.PostTagService;
import com.lq.travel.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private PostTagMapper postTagMapper;

    @Resource
    private PostTagService postTagService;

    @Override
    public List<Tag> getTagsByPostId(Long postId) {
        return tagMapper.selectTagsByPostId(postId);
    }

    @Override
    public List<Tag> searchTags(String keyword) {
        return tagMapper.searchTags(keyword);
    }

    @Override
    @Transactional
    public List<Tag> createOrGetTags(List<String> tagNames) {
        List<Tag> result = new ArrayList<>();

        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) {
                continue;
            }

            String normalizedName = tagName.trim();
            QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", normalizedName);
            queryWrapper.eq("is_delete", 0);

            Tag existingTag = this.getOne(queryWrapper);

            if (existingTag != null) {
                result.add(existingTag);
            } else {
                Tag newTag = new Tag();
                newTag.setName(normalizedName);
                this.save(newTag);
                result.add(newTag);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void updatePostTags(Long postId, List<String> tagNames) {
        // 删除旧的关联关系
        postTagService.deleteByPostId(postId);

        if (tagNames != null && !tagNames.isEmpty()) {
            // 创建或获取标签
            List<Tag> tags = createOrGetTags(tagNames);

            // 创建新的关联关系
            for (Tag tag : tags) {
                PostTag postTag = new PostTag();
                postTag.setPostId(postId);
                postTag.setTagId(tag.getId());
                postTagMapper.insert(postTag);
            }
        }
    }
}