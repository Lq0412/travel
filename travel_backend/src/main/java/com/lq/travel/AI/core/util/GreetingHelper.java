package com.lq.travel.AI.core.util;

import java.util.Calendar;

/**
 * 问候语生成工具类
 * 根据时间生成相应的问候语
 */
public class GreetingHelper {
    
    /**
     * 生成从化旅游助手的问候语
     * 
     * @return 完整的问候语
     */
    public static String generateTourismGreeting() {
        String timeGreeting = getTimeBasedGreeting();
        return timeGreeting + " 我是从化旅游助手！我可以为您提供从化地区的景点推荐、行程规划、美食介绍等服务。"
                + "请问您有什么旅游需求？";
    }
    
    /**
     * 根据当前时间生成时间问候语
     * 
     * @return 时间问候语（带emoji）
     */
    public static String getTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) return "🌅 早上好！";
        if (hour >= 12 && hour < 18) return "☀️ 下午好！";
        if (hour >= 18 && hour < 22) return "🌇 晚上好！";
        return "🌙 您好！";
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private GreetingHelper() {
        throw new AssertionError("GreetingHelper不应该被实例化");
    }
}

