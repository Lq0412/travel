package com.lq.travel.service;

import com.lq.travel.model.vo.ContentImageVO;

import java.util.List;

/**
 * 页面内容图片服务
 */
public interface ContentImageService {

    /**
     * 搜索旅行内容图片
     */
    List<ContentImageVO> searchTravelImages(String query, Integer perPage);
}
