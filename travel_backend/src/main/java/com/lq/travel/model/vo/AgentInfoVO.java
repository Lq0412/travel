package com.lq.travel.model.vo;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代理信息视图对象
 * 用于返回代理信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentInfoVO {
    
    /**
     * 代理名称
     */
    private String name;
    
    /**
     * 代理描述
     */
    private String description;
    
    /**
     * 是否可用
     */
    private Boolean available;
    
    /**
     * 代理能力列表
     */
    private List<String> capabilities;
    
    /**
     * 执行任务数量
     */
    private Integer taskCount;
    
    /**
     * 平均执行时间(毫秒)
     */
    private Long averageExecutionTime;
    
    /**
     * 成功率
     */
    private Double successRate;
}
