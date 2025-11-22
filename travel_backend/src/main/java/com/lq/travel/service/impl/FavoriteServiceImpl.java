package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lq.travel.mapper.FavoriteMapper;
import com.lq.travel.model.entity.Favorite;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.FavoriteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private FavoriteMapper favoriteMapper;

    @Override
    public void addFavorite(Long postId, User loginUser) {
        if (isFavorited(postId, loginUser.getId())) {
            throw new RuntimeException("已经收藏过了");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(loginUser.getId());
        favorite.setTargetId(postId);
        favorite.setType(1); // 1表示帖子

        favoriteMapper.insert(favorite);
    }

    @Override
    public void removeFavorite(Long postId, User loginUser) {
        if (!isFavorited(postId, loginUser.getId())) {
            throw new RuntimeException("尚未收藏");
        }

        QueryWrapper<Favorite> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("target_id", postId);
        queryWrapper.eq("type", 1);

        favoriteMapper.delete(queryWrapper);
    }

    @Override
    public boolean isFavorited(Long postId, Long userId) {
        QueryWrapper<Favorite> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("target_id", postId);
        queryWrapper.eq("type", 1);

        return favoriteMapper.selectCount(queryWrapper) > 0;
    }
}