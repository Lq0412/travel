package com.lq.travel.service;


import com.lq.travel.model.entity.User;

public interface LikeService {
    void likePost(Long postId, User loginUser);
    void unlikePost(Long postId, User loginUser);
    boolean isLiked(Long postId, Long userId);
}