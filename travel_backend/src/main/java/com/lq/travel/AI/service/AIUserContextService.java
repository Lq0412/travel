package com.lq.travel.AI.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 模块用户上下文服务
 * 统一处理 AI 模块内的登录用户获取和角色判断，避免 AI 核心层直接依赖用户实体。
 */
public interface AIUserContextService {

    /**
     * 从会话中提取当前登录用户 ID。
     *
     * @param request HTTP 请求
     * @return 登录用户 ID，不存在时返回 null
     */
    Long extractLoginUserId(HttpServletRequest request);

    /**
     * 判断用户是否为管理员。
     *
     * @param userId 用户 ID
     * @return true 表示管理员
     */
    boolean isAdmin(Long userId);
}
