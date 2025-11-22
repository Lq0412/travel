package com.lq.travel.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class MessageWallCacheUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String MESSAGE_WALL_KEY = "message:wall:";
    private static final String BACKGROUND_KEY = "background:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    public void cacheMessageWall(Long scenicSpotId, Object data) {
        String key = MESSAGE_WALL_KEY + scenicSpotId;
        redisTemplate.opsForValue().set(key, data, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    public Object getCachedMessageWall(Long scenicSpotId) {
        String key = MESSAGE_WALL_KEY + scenicSpotId;
        return redisTemplate.opsForValue().get(key);
    }

    public void clearMessageWallCache(Long scenicSpotId) {
        String key = MESSAGE_WALL_KEY + scenicSpotId;
        String configKey = MESSAGE_WALL_KEY + "config:" + scenicSpotId;
        redisTemplate.delete(key);
        redisTemplate.delete(configKey);
    }

    /**
     * 缓存景点留言墙配置信息
     */
    public void cacheScenicMessageWallConfig(Long scenicSpotId, Object config) {
        String key = MESSAGE_WALL_KEY + "config:" + scenicSpotId;
        redisTemplate.opsForValue().set(key, config, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存的景点留言墙配置
     */
    public Object getCachedScenicMessageWallConfig(Long scenicSpotId) {
        String key = MESSAGE_WALL_KEY + "config:" + scenicSpotId;
        return redisTemplate.opsForValue().get(key);
    }
}