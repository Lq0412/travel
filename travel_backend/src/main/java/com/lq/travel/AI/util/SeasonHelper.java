package com.lq.travel.AI.util;

import java.util.Calendar;

/**
 * 季节辅助工具类
 * 提供季节判断和季节特点查询功能
 */
public class SeasonHelper {
    
    /**
     * 获取当前季节
     * 
     * @return 当前季节（春季/夏季/秋季/冬季）
     */
    public static String getCurrentSeason() {
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }
    
    /**
     * 获取指定季节的特点和推荐景点
     * 
     * @param season 季节名称
     * @return 季节特点描述
     */
    public static String getSeasonFeatures(String season) {
        if (season == null) {
            return "气候适中，推荐景点: 北回归线标志塔、三桠塘幽谷";
        }
        
        return switch (season) {
            case "春季" -> "气候宜人，百花盛开，推荐景点: 石门国家森林公园(杜鹃花海)、溪头村(李花)";
            case "夏季" -> "炎热多雨，推荐景点: 千泷沟大瀑布(避暑)、流溪河(水上活动)";
            case "秋季" -> "凉爽干燥，推荐景点: 石门国家森林公园(红叶)、天湖旅游风景区";
            case "冬季" -> "温和少雨，推荐景点: 碧水湾温泉、温泉旅游区";
            default -> "气候适中，推荐景点: 北回归线标志塔、三桠塘幽谷";
        };
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private SeasonHelper() {
        throw new AssertionError("SeasonHelper不应该被实例化");
    }
}

