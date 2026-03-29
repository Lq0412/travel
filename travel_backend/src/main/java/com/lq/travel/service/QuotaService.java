package com.lq.travel.service;

/**
 * 配额管理服务
 * 管理用户的AI使用配额
 */
public interface QuotaService {
    
    /**
     * 检查用户是否有足够配额
     * 
     * @param userId 用户ID
     * @return true=有配额，false=配额不足
     */
    boolean checkQuota(Long userId);
    
    /**
     * 扣减用户配额
     * 
     * @param userId 用户ID
     * @param tokens 使用的Token数量
     */
    void deductQuota(Long userId, Integer tokens);
    
    /**
     * 获取用户剩余配额
     * 
     * @param userId 用户ID
     * @return 剩余配额（Token数量）
     */
    Integer getRemainingQuota(Long userId);
    
    /**
     * 重置用户配额（管理员功能）
     * 
     * @param userId 用户ID
     * @param quota 新配额
     */
    void resetQuota(Long userId, Integer quota);
    
    /**
     * 为用户充值配额
     * 
     * @param userId 用户ID
     * @param tokens 充值Token数量
     */
    void rechargeQuota(Long userId, Integer tokens);
}

