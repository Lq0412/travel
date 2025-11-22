package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.PostTagMapper;
import com.lq.travel.model.entity.PostTag;
import com.lq.travel.service.PostTagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostTagServiceImpl extends ServiceImpl<PostTagMapper, PostTag> implements PostTagService {

    @Resource
    private PostTagMapper postTagMapper;

    @Override
    public void deleteByPostId(Long postId) {
        postTagMapper.deleteByPostId(postId);
    }

    @Override
    public List<Long> getPostIdsByTagId(Long tagId) {
        return postTagMapper.selectPostIdsByTagId(tagId);
    }

    @Override
    public List<Long> getPostIdsByTagIds(List<Long> tagIds) {
        return postTagMapper.selectPostIdsByTagIds(tagIds);
    }
}