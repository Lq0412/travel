package com.lq.travel.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户更新请求
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 可选：用于兼容前端传参，但最终以登录用户 ID 为准
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 个人简介
     */
    private String userProfile;
}


