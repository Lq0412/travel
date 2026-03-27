package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.AI.core.model.AIRequest;
import com.lq.travel.AI.core.model.AIResponse;
import com.lq.travel.AI.core.service.AIService;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.MemoryCardMapper;
import com.lq.travel.mapper.PostMapper;
import com.lq.travel.mapper.TripMapper;
import com.lq.travel.model.dto.trip.TripGenerateRequest;
import com.lq.travel.model.dto.trip.TripGenerateResponse;
import com.lq.travel.model.dto.trip.TripSaveRequest;
import com.lq.travel.model.entity.MemoryCard;
import com.lq.travel.model.entity.Post;
import com.lq.travel.model.entity.Trip;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MemoryCardVO;
import com.lq.travel.model.vo.TripPhotoVO;
import com.lq.travel.model.vo.TripVO;
import com.lq.travel.service.TripPhotoService;
import com.lq.travel.service.TripService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 行程服务实现类
 */
@Slf4j
@Service
public class TripServiceImpl extends ServiceImpl<TripMapper, Trip> implements TripService {

    private static final String STATUS_PLANNED = "planned";
    private static final String STATUS_COMPLETED = "completed";
    
    @Resource
    private AIService aiService;
    
    @Autowired
    @Lazy
    private TripPhotoService tripPhotoService;

    @Resource
    private MemoryCardMapper memoryCardMapper;

    @Resource
    private PostMapper postMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public TripGenerateResponse generateTripPlans(TripGenerateRequest request, User user) {
        // 参数校验
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目的地不能为空");
        }
        if (request.getDays() == null || request.getDays() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "天数必须大于0");
        }
        
        // 构建AI提示词
        String prompt = buildTripGenerationPrompt(request);
        
        // 调用AI生成行程
        AIRequest aiRequest = AIRequest.builder()
                .message(prompt)
                .systemPrompt("你是一个专业的旅行规划师，擅长根据用户需求生成详细的旅行行程方案。请以JSON格式返回结果。")
                .maxTokens(2000)
                .temperature(0.7)
                .build();
        
        AIResponse aiResponse = aiService.chat(aiRequest);
        
        if (!aiResponse.getSuccess()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成行程失败: " + aiResponse.getErrorMessage());
        }
        
        // 解析AI返回的JSON
        TripGenerateResponse response = parseAIResponse(aiResponse.getContent(), request);
        
        return response;
    }
    
    /**
     * 构建行程生成提示词
     */
    private String buildTripGenerationPrompt(TripGenerateRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为我生成").append(request.getDays()).append("天的").append(request.getDestination()).append("旅行行程方案。");
        
        if (request.getBudget() != null && request.getBudget() > 0) {
            prompt.append("预算：").append(request.getBudget()).append("元。");
        }
        
        if (request.getTheme() != null && !request.getTheme().trim().isEmpty()) {
            prompt.append("主题：").append(request.getTheme()).append("。");
        }
        
        prompt.append("\n\n请生成1-2个详细的行程方案，每个方案需要包含：\n");
        prompt.append("1. 每天的行程亮点（景点、活动等）\n");
        prompt.append("2. 简要描述\n\n");
        prompt.append("请以以下JSON格式返回（如果生成2个方案，返回数组；如果1个方案，返回单个对象）：\n");
        prompt.append("{\n");
        prompt.append("  \"planId\": \"plan_1\",\n");
        prompt.append("  \"destination\": \"").append(request.getDestination()).append("\",\n");
        prompt.append("  \"days\": ").append(request.getDays()).append(",\n");
        prompt.append("  \"budget\": ").append(request.getBudget() != null ? request.getBudget() : 0).append(",\n");
        prompt.append("  \"theme\": \"").append(request.getTheme() != null ? request.getTheme() : "").append("\",\n");
        prompt.append("  \"description\": \"方案描述\",\n");
        prompt.append("  \"dailyHighlights\": {\n");
        prompt.append("    \"1\": [\"第一天亮点1\", \"第一天亮点2\"],\n");
        prompt.append("    \"2\": [\"第二天亮点1\", \"第二天亮点2\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("注意：dailyHighlights的key是字符串格式的数字（1, 2, 3...），value是字符串数组。");
        
        return prompt.toString();
    }
    
    /**
     * 解析AI返回的JSON响应
     */
    private TripGenerateResponse parseAIResponse(String content, TripGenerateRequest request) {
        TripGenerateResponse response = new TripGenerateResponse();
        List<TripGenerateResponse.TripPlan> plans = new ArrayList<>();
        
        try {
            // 尝试解析JSON（可能是数组或单个对象）
            String jsonContent = extractJsonFromResponse(content);
            
            // 先尝试解析为数组
            try {
                List<Map<String, Object>> planList = objectMapper.readValue(jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {});
                
                for (int i = 0; i < planList.size(); i++) {
                    Map<String, Object> planMap = planList.get(i);
                    TripGenerateResponse.TripPlan plan = convertToPlan(planMap, request, i + 1);
                    plans.add(plan);
                }
            } catch (Exception e) {
                // 如果不是数组，尝试解析为单个对象
                Map<String, Object> planMap = objectMapper.readValue(jsonContent, 
                    new TypeReference<Map<String, Object>>() {});
                TripGenerateResponse.TripPlan plan = convertToPlan(planMap, request, 1);
                plans.add(plan);
            }
            
            response.setPlans(plans);
            return response;
            
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            // 如果解析失败，创建一个默认方案
            TripGenerateResponse.TripPlan defaultPlan = createDefaultPlan(request);
            plans.add(defaultPlan);
            response.setPlans(plans);
            return response;
        }
    }
    
    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJsonFromResponse(String content) {
        // 尝试提取JSON代码块
        int jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return content.substring(jsonStart, jsonEnd + 1);
        }
        
        return content;
    }
    
    /**
     * 转换为行程方案对象
     */
    @SuppressWarnings("unchecked")
    private TripGenerateResponse.TripPlan convertToPlan(Map<String, Object> planMap, 
                                                         TripGenerateRequest request, int index) {
        TripGenerateResponse.TripPlan plan = new TripGenerateResponse.TripPlan();
        plan.setPlanId("plan_" + index);
        plan.setDestination(request.getDestination());
        plan.setDays(request.getDays());
        plan.setBudget(request.getBudget());
        plan.setTheme(request.getTheme());
        
        if (planMap.containsKey("description")) {
            plan.setDescription(String.valueOf(planMap.get("description")));
        }
        
        // 解析dailyHighlights
        Map<Integer, List<String>> dailyHighlights = new HashMap<>();
        if (planMap.containsKey("dailyHighlights")) {
            Object highlightsObj = planMap.get("dailyHighlights");
            if (highlightsObj instanceof Map) {
                Map<String, Object> highlightsMap = (Map<String, Object>) highlightsObj;
                for (Map.Entry<String, Object> entry : highlightsMap.entrySet()) {
                    try {
                        int day = Integer.parseInt(entry.getKey());
                        if (entry.getValue() instanceof List) {
                            List<String> highlights = ((List<?>) entry.getValue()).stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.toList());
                            dailyHighlights.put(day, highlights);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析天数: {}", entry.getKey());
                    }
                }
            }
        }
        plan.setDailyHighlights(dailyHighlights);
        
        return plan;
    }
    
    /**
     * 创建默认方案（当AI解析失败时）
     */
    private TripGenerateResponse.TripPlan createDefaultPlan(TripGenerateRequest request) {
        TripGenerateResponse.TripPlan plan = new TripGenerateResponse.TripPlan();
        plan.setPlanId("plan_1");
        plan.setDestination(request.getDestination());
        plan.setDays(request.getDays());
        plan.setBudget(request.getBudget());
        plan.setTheme(request.getTheme());
        plan.setDescription("AI生成失败，请稍后重试或手动创建行程");
        
        Map<Integer, List<String>> dailyHighlights = new HashMap<>();
        for (int i = 1; i <= request.getDays(); i++) {
            dailyHighlights.put(i, Arrays.asList("待规划"));
        }
        plan.setDailyHighlights(dailyHighlights);
        
        return plan;
    }
    
    @Override
    @Transactional
    public Long saveTrip(TripSaveRequest request, User user) {
        // 参数校验
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目的地不能为空");
        }
        
        // 创建行程实体
        Trip trip = new Trip();
        trip.setUserId(user.getId());
        trip.setDestination(request.getDestination());
        trip.setDays(request.getDays());
        trip.setBudget(request.getBudget());
        trip.setTheme(request.getTheme());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setStatus(STATUS_PLANNED);
        
        // 将dailyHighlights转换为JSON字符串
        try {
            if (request.getDailyHighlights() != null) {
                String dailyHighlightsJson = objectMapper.writeValueAsString(request.getDailyHighlights());
                trip.setDailyHighlights(dailyHighlightsJson);
            }
        } catch (Exception e) {
            log.error("转换dailyHighlights失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存行程失败");
        }
        
        // 保存完整的结构化数据
        if (request.getStructuredData() != null && !request.getStructuredData().trim().isEmpty()) {
            trip.setStructuredData(request.getStructuredData());
            log.info("保存完整结构化数据，长度: {} 字符", request.getStructuredData().length());
        }
        
        // 保存行程
        this.save(trip);
        
        return trip.getId();
    }
    
    @Override
    public List<TripVO> getUserTrips(Long userId) {
        QueryWrapper<Trip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("update_time");
        queryWrapper.orderByDesc("create_time");
        
        List<Trip> trips = this.list(queryWrapper);
        
        return trips.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public TripVO getTripById(Long tripId, Long userId) {
        Trip trip = this.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该行程");
        }
        
        return convertToVO(trip);
    }
    
    @Override
    @Transactional
    public void completeTrip(Long tripId, Long userId) {
        Trip trip = this.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该行程");
        }
        
        trip.setStatus("completed");
        this.updateById(trip);
    }
    
    @Override
    @Transactional
    public void deleteTrip(Long tripId, Long userId) {
        Trip trip = this.getById(tripId);
        if (trip == null || trip.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "行程不存在");
        }
        
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该行程");
        }
        
        this.removeById(tripId);
    }
    
    /**
     * 转换为VO对象
     */
    private TripVO convertToVO(Trip trip) {
        TripVO vo = new TripVO();
        BeanUtils.copyProperties(trip, vo);
        vo.setStatus(normalizeTripStatus(trip.getStatus()));
        
        // 解析dailyHighlights JSON
        try {
            if (trip.getDailyHighlights() != null && !trip.getDailyHighlights().trim().isEmpty()) {
                Map<String, List<String>> highlightsMap = objectMapper.readValue(
                    trip.getDailyHighlights(), 
                    new TypeReference<Map<String, List<String>>>() {});
                
                // 转换为Integer key的Map
                Map<Integer, List<String>> dailyHighlights = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : highlightsMap.entrySet()) {
                    try {
                        int day = Integer.parseInt(entry.getKey());
                        dailyHighlights.put(day, entry.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("无法解析天数: {}", entry.getKey());
                    }
                }
                vo.setDailyHighlights(dailyHighlights);
            }
        } catch (Exception e) {
            log.error("解析dailyHighlights失败", e);
        }
        
        // 获取照片列表
        List<TripPhotoVO> photos = tripPhotoService.getTripPhotos(trip.getId());
        vo.setPhotos(photos);

        MemoryCard currentMemoryCard = loadCurrentMemoryCard(trip.getId());
        if (currentMemoryCard != null) {
            vo.setMemoryCard(convertMemoryCardToVO(currentMemoryCard));

            Post publishedPost = findPublishedPost(trip.getUserId(), currentMemoryCard.getImageUrl());
            if (publishedPost != null) {
                vo.setPublishedToInspiration(Boolean.TRUE);
                vo.setPublishedPostId(publishedPost.getId());
            } else {
                vo.setPublishedToInspiration(Boolean.FALSE);
            }
        } else {
            vo.setPublishedToInspiration(Boolean.FALSE);
        }
        
        return vo;
    }

    private String normalizeTripStatus(String status) {
        return STATUS_COMPLETED.equals(status) ? STATUS_COMPLETED : STATUS_PLANNED;
    }

    private MemoryCard loadCurrentMemoryCard(Long tripId) {
        return memoryCardMapper.selectOne(new QueryWrapper<MemoryCard>()
                .eq("trip_id", tripId)
                .eq("is_delete", 0)
                .orderByDesc("create_time")
                .last("limit 1"));
    }

    private MemoryCardVO convertMemoryCardToVO(MemoryCard memoryCard) {
        MemoryCardVO vo = new MemoryCardVO();
        BeanUtils.copyProperties(memoryCard, vo);
        return vo;
    }

    private Post findPublishedPost(Long userId, String coverUrl) {
        if (!StringUtils.hasText(coverUrl)) {
            return null;
        }
        return postMapper.selectOne(new QueryWrapper<Post>()
                .eq("user_id", userId)
                .eq("cover_url", coverUrl)
                .orderByDesc("create_time")
                .last("limit 1"));
    }
}

