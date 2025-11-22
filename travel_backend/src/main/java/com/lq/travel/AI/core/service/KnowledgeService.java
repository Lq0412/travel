package com.lq.travel.AI.core.service;

import com.lq.travel.AI.core.model.entity.KnowledgeAttraction;
import com.lq.travel.AI.core.model.entity.KnowledgeFood;

import java.util.List;

/**
 * 知识库服务接口
 * 提供景点、美食等知识的查询和推荐功能
 */
public interface KnowledgeService {
    
    /**
     * 根据季节获取推荐景点
     * 
     * @param season 季节（春/夏/秋/冬/全年）
     * @return 景点列表
     */
    List<KnowledgeAttraction> getAttractionsBySeason(String season);
    
    /**
     * 根据类型获取景点
     * 
     * @param category 类型（自然/文化/温泉等）
     * @return 景点列表
     */
    List<KnowledgeAttraction> getAttractionsByCategory(String category);
    
    /**
     * 根据适合人群获取景点
     * 
     * @param suitableFor 适合人群
     * @return 景点列表
     */
    List<KnowledgeAttraction> getAttractionsBySuitableFor(String suitableFor);
    
    /**
     * 根据价格等级获取景点
     * 
     * @param maxPriceLevel 最大价格等级
     * @return 景点列表
     */
    List<KnowledgeAttraction> getAttractionsByPrice(Integer maxPriceLevel);
    
    /**
     * 多条件筛选景点
     * 
     * @param season 季节（可选）
     * @param category 类型（可选）
     * @param suitableFor 适合人群（可选）
     * @param maxPriceLevel 最大价格等级（可选）
     * @return 景点列表
     */
    List<KnowledgeAttraction> searchAttractions(String season, String category, 
                                                 String suitableFor, Integer maxPriceLevel);
    
    /**
     * 获取所有景点（带缓存）
     * 
     * @return 景点列表
     */
    List<KnowledgeAttraction> getAllAttractions();
    
    /**
     * 获取所有美食（带缓存）
     * 
     * @return 美食列表
     */
    List<KnowledgeFood> getAllFoods();
    
    /**
     * 根据名称搜索景点
     * 
     * @param name 景点名称关键词
     * @return 景点列表
     */
    List<KnowledgeAttraction> searchAttractionsByName(String name);
    
    /**
     * 语义搜索景点（基于描述、标签等）
     * 
     * @param query 用户查询
     * @return 景点列表
     */
    List<KnowledgeAttraction> semanticSearchAttractions(String query);
    
    /**
     * 获取景点的详细描述文本（用于AI对话）
     * 
     * @param attraction 景点实体
     * @return 格式化的描述文本
     */
    String formatAttractionForAI(KnowledgeAttraction attraction);
    
    /**
     * 获取美食的详细描述文本（用于AI对话）
     * 
     * @param food 美食实体
     * @return 格式化的描述文本
     */
    String formatFoodForAI(KnowledgeFood food);
    
    /**
     * 批量构建知识库上下文（用于AI提示词）
     * 
     * @param season 当前季节
     * @param userPreference 用户偏好
     * @return 知识库上下文文本
     */
    String buildKnowledgeContext(String season, String userPreference);
}

