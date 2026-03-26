package com.lq.travel.controller;

import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.vo.ContentImageVO;
import com.lq.travel.service.ContentImageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 页面内容图片控制器
 */
@RestController
@RequestMapping("/content/images")
public class ContentImageController {

    @Resource
    private ContentImageService contentImageService;

    /**
     * 搜索 Pexels 图片
     */
    @GetMapping("/search")
    public com.lq.travel.AI.core.model.dto.ResponseDTO<List<ContentImageVO>> searchImages(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "6") Integer perPage) {
        return ResponseUtils.success(contentImageService.searchTravelImages(query, perPage));
    }
}
