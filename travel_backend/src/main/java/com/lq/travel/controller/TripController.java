package com.lq.travel.controller;

import com.lq.travel.AI.core.annotation.ApiRateLimit;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.trip.ForumPublishRequest;
import com.lq.travel.model.dto.trip.TripGenerateRequest;
import com.lq.travel.model.dto.trip.TripGenerateResponse;
import com.lq.travel.model.dto.trip.TripSaveRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.TripVO;
import com.lq.travel.service.MemoryCardService;
import com.lq.travel.service.PostService;
import com.lq.travel.service.TripService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行程控制器
 */
@RestController
@RequestMapping("/ai/trips")
@Slf4j
public class TripController {
    
    @Resource
    private TripService tripService;
    
    @Resource
    private UserService userService;
    
    @Resource
    private MemoryCardService memoryCardService;
    
    @Resource
    private PostService postService;
    
    /**
     * AI生成行程方案
     */
    @PostMapping("/generate")
    @AuthCheck
    @ApiRateLimit(name = "aiChat")
    public com.lq.travel.AI.core.model.dto.ResponseDTO<TripGenerateResponse> generateTrip(
            @RequestBody TripGenerateRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        TripGenerateResponse response = tripService.generateTripPlans(request, loginUser);
        return ResponseUtils.success(response);
    }
    
    /**
     * 保存行程（接受方案）
     */
    @PostMapping("/save")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Long> saveTrip(
            @RequestBody TripSaveRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long tripId = tripService.saveTrip(request, loginUser);
        return ResponseUtils.success(tripId);
    }
    
    /**
     * 获取我的行程列表
     */
    @GetMapping("/my")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<List<TripVO>> getMyTrips(
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<TripVO> trips = tripService.getUserTrips(loginUser.getId());
        return ResponseUtils.success(trips);
    }
    
    /**
     * 获取行程详情
     */
    @GetMapping("/{id}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<TripVO> getTripById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        TripVO trip = tripService.getTripById(id, loginUser.getId());
        return ResponseUtils.success(trip);
    }
    
    /**
     * 标记行程为已完成
     */
    @PostMapping("/{id}/complete")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Boolean> completeTrip(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        tripService.completeTrip(id, loginUser.getId());
        return ResponseUtils.success(true);
    }
    
    /**
     * 删除行程
     */
    @DeleteMapping("/{id}")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Boolean> deleteTrip(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        tripService.deleteTrip(id, loginUser.getId());
        return ResponseUtils.success(true);
    }
    
    /**
     * 一键发布论坛
     */
    @PostMapping("/{id}/publish-forum")
    @AuthCheck
    public com.lq.travel.AI.core.model.dto.ResponseDTO<Long> publishToForum(
            @PathVariable Long id,
            @RequestBody(required = false) ForumPublishRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        
        // 获取行程信息
        TripVO trip = tripService.getTripById(id, loginUser.getId());
        
        // 获取回忆图
        com.lq.travel.model.vo.MemoryCardVO memoryCard = memoryCardService.getMemoryCardByTripId(id, loginUser.getId());
        if (memoryCard == null || !"success".equals(memoryCard.getStatus())) {
            return ResponseUtils.error("请先生成回忆图");
        }
        
        // 构建论坛帖子内容
        String title = request != null && request.getTitle() != null 
            ? request.getTitle() 
            : generateDefaultTitle(trip);
        
        String content = request != null && request.getContent() != null
            ? request.getContent()
            : generateDefaultContent(trip, memoryCard);
        
        // 创建帖子
        com.lq.travel.model.entity.Post post = new com.lq.travel.model.entity.Post();
        post.setUserId(loginUser.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(memoryCard.getImageUrl());
        post.setCategoryId(request != null && request.getCategoryId() != null 
            ? request.getCategoryId() 
            : getDefaultCategoryId());
        post.setStatus(1);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        
        // 生成标签
        List<String> tags = request != null && request.getTags() != null
            ? request.getTags()
            : generateDefaultTags(trip);
        
        // 发布帖子
        Long postId = postService.addPost(post, tags);
        
        return ResponseUtils.success(postId);
    }
    
    /**
     * 生成默认标题
     */
    private String generateDefaultTitle(TripVO trip) {
        String emoji = "✈️"; // 默认emoji
        return trip.getDestination() + " " + trip.getDays() + "天 " + emoji;
    }
    
    /**
     * 生成默认内容
     */
    private String generateDefaultContent(TripVO trip, com.lq.travel.model.vo.MemoryCardVO memoryCard) {
        StringBuilder content = new StringBuilder();
        content.append("📍 目的地：").append(trip.getDestination()).append("\n\n");
        content.append("⏰ 时间：").append(trip.getDays()).append("天\n\n");
        
        if (trip.getDailyHighlights() != null && !trip.getDailyHighlights().isEmpty()) {
            content.append("✨ 行程亮点：\n");
            int count = 0;
            for (List<String> highlights : trip.getDailyHighlights().values()) {
                if (count >= 3) break;
                for (String highlight : highlights) {
                    if (count >= 3) break;
                    content.append("• ").append(highlight).append("\n");
                    count++;
                }
            }
            content.append("\n");
        }
        
        content.append("📸 回忆图：\n");
        content.append("使用AI生成的旅行回忆卡片，记录这次美好的旅程！\n\n");
        content.append("💡 Tips：\n");
        content.append("• 建议提前预订酒店和景点门票\n");
        content.append("• 注意天气变化，准备合适的衣物\n");
        content.append("• 保持手机电量充足，记录美好瞬间");
        
        return content.toString();
    }
    
    /**
     * 生成默认标签
     */
    private List<String> generateDefaultTags(TripVO trip) {
        List<String> tags = new java.util.ArrayList<>();
        tags.add(trip.getDestination());
        tags.add(trip.getDays() + "天");
        tags.add("#AI生成");
        return tags;
    }
    
    /**
     * 获取默认分类ID（游记/旅游分享）
     */
    private Integer getDefaultCategoryId() {
        // TODO: 从数据库查询"游记"或"旅游分享"分类
        // 如果没有，返回第一个分类ID
        return 1; // 临时返回1，实际应从数据库查询
    }
}

