package com.lq.travel.AI.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lq.travel.AI.core.mapper.KnowledgeAttractionMapper;
import com.lq.travel.AI.core.mapper.KnowledgeFoodMapper;
import com.lq.travel.AI.core.model.entity.KnowledgeAttraction;
import com.lq.travel.AI.core.model.entity.KnowledgeFood;
import com.lq.travel.AI.core.service.KnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识库服务实现
 */
@Service
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {
    
    @Resource
    private KnowledgeAttractionMapper attractionMapper;
    
    @Resource
    private KnowledgeFoodMapper foodMapper;
    
    @Override
    @Cacheable(value = "knowledge:attraction:season", key = "#season")
    public List<KnowledgeAttraction> getAttractionsBySeason(String season) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(KnowledgeAttraction::getSeason, season)
                         .or()
                         .eq(KnowledgeAttraction::getSeason, "全年"));
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    @Cacheable(value = "knowledge:attraction:category", key = "#category")
    public List<KnowledgeAttraction> getAttractionsByCategory(String category) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeAttraction::getCategory, category);
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    public List<KnowledgeAttraction> getAttractionsBySuitableFor(String suitableFor) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(KnowledgeAttraction::getSuitableFor, suitableFor);
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    public List<KnowledgeAttraction> getAttractionsByPrice(Integer maxPriceLevel) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(KnowledgeAttraction::getPriceLevel, maxPriceLevel);
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    public List<KnowledgeAttraction> searchAttractions(String season, String category, 
                                                        String suitableFor, Integer maxPriceLevel) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(season)) {
            wrapper.and(w -> w.eq(KnowledgeAttraction::getSeason, season)
                             .or()
                             .eq(KnowledgeAttraction::getSeason, "全年"));
        }
        
        if (StringUtils.hasText(category)) {
            wrapper.eq(KnowledgeAttraction::getCategory, category);
        }
        
        if (StringUtils.hasText(suitableFor)) {
            wrapper.like(KnowledgeAttraction::getSuitableFor, suitableFor);
        }
        
        if (maxPriceLevel != null) {
            wrapper.le(KnowledgeAttraction::getPriceLevel, maxPriceLevel);
        }
        
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    @Cacheable(value = "knowledge:attraction:all")
    public List<KnowledgeAttraction> getAllAttractions() {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeAttraction::getRating);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    @Cacheable(value = "knowledge:food:all")
    public List<KnowledgeFood> getAllFoods() {
        return foodMapper.selectList(null);
    }
    
    @Override
    public List<KnowledgeAttraction> searchAttractionsByName(String name) {
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(KnowledgeAttraction::getName, name);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    public List<KnowledgeAttraction> semanticSearchAttractions(String query) {
        // TODO: 实现语义搜索（可选）
        // 1. 使用向量数据库检索
        // 2. 或使用ElasticSearch全文检索
        // 3. 或使用AI嵌入向量相似度匹配
        
        // 当前简化实现：关键词匹配
        LambdaQueryWrapper<KnowledgeAttraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(KnowledgeAttraction::getName, query)
               .or()
               .like(KnowledgeAttraction::getDescription, query);
        return attractionMapper.selectList(wrapper);
    }
    
    @Override
    public String formatAttractionForAI(KnowledgeAttraction attraction) {
        StringBuilder sb = new StringBuilder();
        sb.append(attraction.getName()).append(": ");
        sb.append(attraction.getDescription());
        
        if (attraction.getFeatures() != null && !attraction.getFeatures().isEmpty()) {
            sb.append(" 特色：").append(String.join("、", attraction.getFeatures()));
        }
        
        if (attraction.getTicketPrice() != null) {
            sb.append(" 门票：").append(attraction.getTicketPrice());
        }
        
        return sb.toString();
    }
    
    @Override
    public String formatFoodForAI(KnowledgeFood food) {
        StringBuilder sb = new StringBuilder();
        sb.append(food.getName()).append(": ");
        sb.append(food.getDescription());
        
        if (food.getWhereToEat() != null) {
            sb.append(" 推荐餐厅：").append(food.getWhereToEat());
        }
        
        return sb.toString();
    }
    
    @Override
    public String buildKnowledgeContext(String season, String userPreference) {
        StringBuilder context = new StringBuilder();
        
        // 根据季节和偏好筛选景点
        List<KnowledgeAttraction> attractions = searchAttractions(season, null, userPreference, null);
        
        if (!attractions.isEmpty()) {
            context.append("主要景点:\n");
            attractions.stream()
                .limit(10)  // 限制数量，避免提示词过长
                .forEach(a -> context.append("- ").append(formatAttractionForAI(a)).append("\n"));
        }
        
        // 获取美食信息
        List<KnowledgeFood> foods = getAllFoods();
        if (!foods.isEmpty()) {
            context.append("\n特色美食:\n");
            foods.stream()
                .limit(10)
                .forEach(f -> context.append("- ").append(formatFoodForAI(f)).append("\n"));
        }
        
        return context.toString();
    }
}

