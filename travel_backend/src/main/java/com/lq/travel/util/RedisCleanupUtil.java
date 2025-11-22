package com.lq.travel.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 清理工具类
 * 用于清理旧的 Session 数据
 */
@Component
public class RedisCleanupUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 清理所有 Session 相关的数据
     */
    public void cleanupSessions() {
        try {
            // 查找所有 Session 相关的键
            Set<String> sessionKeys = redisTemplate.keys("spring:session:*");
            if (sessionKeys != null && !sessionKeys.isEmpty()) {
                redisTemplate.delete(sessionKeys);
                System.out.println("已清理 " + sessionKeys.size() + " 个 Session 键");
            } else {
                System.out.println("没有找到 Session 相关的键");
            }
        } catch (Exception e) {
            System.err.println("清理 Session 数据时出错: " + e.getMessage());
        }
    }

    /**
     * 清理所有 Redis 数据
     */
    public void cleanupAll() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            if (allKeys != null && !allKeys.isEmpty()) {
                redisTemplate.delete(allKeys);
                System.out.println("已清理所有 Redis 数据，共 " + allKeys.size() + " 个键");
            } else {
                System.out.println("Redis 中没有数据");
            }
        } catch (Exception e) {
            System.err.println("清理 Redis 数据时出错: " + e.getMessage());
        }
    }

    /**
     * 查看 Redis 中的所有键
     */
    public void listAllKeys() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            if (allKeys != null && !allKeys.isEmpty()) {
                System.out.println("Redis 中的所有键:");
                for (String key : allKeys) {
                    System.out.println("  - " + key);
                }
            } else {
                System.out.println("Redis 中没有数据");
            }
        } catch (Exception e) {
            System.err.println("查看 Redis 键时出错: " + e.getMessage());
        }
    }
}

