package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.MessageLike;

public interface MessageLikeService extends IService<MessageLike> {

    boolean checkUserLike(Long userId, Long messageId);

    boolean removeUserLike(Long userId, Long messageId);

    Integer getLikeCount(Long messageId);
}