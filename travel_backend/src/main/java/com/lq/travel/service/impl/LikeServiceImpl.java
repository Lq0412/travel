package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lq.travel.mapper.LikeRecordMapper;
import com.lq.travel.mapper.PostMapper;
import com.lq.travel.model.entity.LikeRecord;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.LikeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl implements LikeService {

    @Resource
    private LikeRecordMapper likeRecordMapper;

    @Resource
    private PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId, User loginUser) {
        // 检查是否已经点赞
        if (isLiked(postId, loginUser.getId())) {
            throw new RuntimeException("已经点赞过了");
        }

        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getIsDelete() == 1) {
            throw new RuntimeException("帖子不存在");
        }

        // 添加点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(loginUser.getId());
        likeRecord.setPostId(postId);
        likeRecordMapper.insert(likeRecord);

        // 更新帖子点赞数
        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(updatePost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long postId, User loginUser) {
        // 检查是否已经点赞
        if (!isLiked(postId, loginUser.getId())) {
            throw new RuntimeException("尚未点赞");
        }

        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getIsDelete() == 1) {
            throw new RuntimeException("帖子不存在");
        }

        // 删除点赞记录
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("post_id", postId);
        likeRecordMapper.delete(queryWrapper);

        // 更新帖子点赞数
        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postMapper.updateById(updatePost);
    }

    @Override
    public boolean isLiked(Long postId, Long userId) {
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("post_id", postId);
        return likeRecordMapper.selectCount(queryWrapper) > 0;
    }
}