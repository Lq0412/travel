package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 任务实体
 * 用于记录异步任务状态（回忆图/行程规划/对话总结等）
 */
@Data
@TableName("ai_task")
public class AiTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对外返回的任务 ID，唯一
     */
    private String taskId;

    /**
     * 幂等业务键，如 userId + type + hash(payload)
     */
    private String bizKey;

    /**
     * 任务类型：MEMORY_CARD / TRIP_PLAN / CHAT_SUMMARY
     */
    private String type;

    /**
     * 入参快照（JSON）
     */
    private String payload;

    /**
     * 状态：PENDING / RUNNING / SUCCESS / FAILED / CANCELLED
     */
    private String status;

    /**
     * 结果数据（JSON），如 COS URL、模型结果等
     */
    private String result;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 最大允许重试次数
     */
    private Integer maxRetry;

    /**
     * 失败原因
     */
    private String errorMessage;

    private Long userId;

    private Long tripId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
