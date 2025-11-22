package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.Tag;
import com.lq.travel.model.vo.PostVO;

import java.util.List;

public interface PostService extends IService<Post> {
    void validPost(Post post, boolean add);

    Page<Post> listPostByPage(Page<Post> page, String sortField);

    Page<Post> listPostByCategory(Page<Post> page, Integer categoryId);

    Post getPostById(Long id, boolean incrementView);

    Page<PostVO> searchPosts(Page<Post> page, String keyword);

    // 新增方法
    Long addPost(Post post, List<String> tags);

    Page<PostVO> searchPostsByTag(Page<Post> page, String tagName);

    PostVO getPostDetailById(Long id, boolean incrementView);

    void updatePostTags(Long postId, List<String> tags);

    Page<PostVO> listPostsByTagId(Page<Post> page, Long tagId);

    Page<PostVO> searchPostsByTagNames(Page<Post> page, List<String> tagNames);

    Page<PostVO> searchPostsByTagIds(Page<Post> page, List<Long> tagIds);

    List<Tag> getHotTags(int limit);
}