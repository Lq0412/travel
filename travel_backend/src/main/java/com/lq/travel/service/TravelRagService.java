package com.lq.travel.service;

import com.lq.travel.model.enums.IntentType;

/**
 * 旅行 RAG 服务
 */
public interface TravelRagService {

    /**
     * 构建可注入到 Prompt 的 RAG 上下文。
     *
     * @param userQuery 用户原始问题
     * @param destinationHint 目的地提示（可空）
     * @param intent 用户意图
     * @return 可直接拼接的上下文，不可用时返回空字符串
     */
    String buildRagContext(String userQuery, String destinationHint, IntentType intent);
}
