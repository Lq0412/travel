package com.lq.travel.service.impl;

import com.lq.travel.service.AIUserContextService;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * AI 模块用户上下文服务实现。
 */
@Service
public class AIUserContextServiceImpl implements AIUserContextService {

    @Resource
    private UserService userService;

    @Override
    public Long extractLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object loginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (!(loginUser instanceof User user)) {
            return null;
        }
        return user.getId();
    }

    @Override
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userService.getById(userId);
        return userService.isAdmin(user);
    }
}
