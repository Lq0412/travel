package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.MessageLikeMapper;
import com.lq.travel.model.entity.MessageLike;
import com.lq.travel.service.MessageLikeService;
import org.springframework.stereotype.Service;

@Service
public class MessageLikeServiceImpl extends ServiceImpl<MessageLikeMapper, MessageLike>
        implements MessageLikeService {

    @Override
    public boolean checkUserLike(Long userId, Long messageId) {
        QueryWrapper<MessageLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("message_id", messageId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean removeUserLike(Long userId, Long messageId) {
        QueryWrapper<MessageLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("message_id", messageId);
        return this.remove(queryWrapper);
    }

    @Override
    public Integer getLikeCount(Long messageId) {
        QueryWrapper<MessageLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_id", messageId);
        return Math.toIntExact(this.count(queryWrapper));
    }
}