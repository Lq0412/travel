package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.PostTag;

import java.util.List;

public interface PostTagService extends IService<PostTag> {

    void deleteByPostId(Long postId);

    List<Long> getPostIdsByTagId(Long tagId);

    List<Long> getPostIdsByTagIds(List<Long> tagIds);
}