package com.lq.travel.AI.core.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 通用响应DTO
 * 用于统一API响应格式
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {
    
    /**
     * 响应码
     * 0 表示成功，其他值表示错误
     */
    @Builder.Default
    private Integer code = 0;
    
    /**
     * 响应消息
     */
    @Builder.Default
    private String message = "success";
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 创建成功响应
     */
    public static <T> ResponseDTO<T> success(T data) {
        return ResponseDTO.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建成功响应（带消息）
     */
    public static <T> ResponseDTO<T> success(String message, T data) {
        return ResponseDTO.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static <T> ResponseDTO<T> error(Integer code, String message) {
        return ResponseDTO.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应（默认500错误码）
     */
    public static <T> ResponseDTO<T> error(String message) {
        return error(500, message);
    }
}
