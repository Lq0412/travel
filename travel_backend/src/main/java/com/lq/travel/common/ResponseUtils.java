package com.lq.travel.common;

import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.exception.ErrorCode;

/**
 * 统一响应体构建工具类（基于 ResponseDTO）
 * 
 * @author Lq304
 * @since 2025-11-03
 */
public class ResponseUtils {
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ResponseDTO<T> success() {
        return ResponseDTO.<T>builder()
                .code(0)
                .message("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 成功响应（带数据）
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
     * 成功响应（带消息和数据）
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
     * 错误响应（使用 ErrorCode）
     */
    public static <T> ResponseDTO<T> error(ErrorCode errorCode) {
        return ResponseDTO.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 错误响应（ErrorCode + 自定义消息）
     */
    public static <T> ResponseDTO<T> error(ErrorCode errorCode, String message) {
        return ResponseDTO.<T>builder()
                .code(errorCode.getCode())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 错误响应（自定义 code 和 message）
     */
    public static <T> ResponseDTO<T> error(int code, String message) {
        return ResponseDTO.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 错误响应（自定义消息，默认500错误码）
     */
    public static <T> ResponseDTO<T> error(String message) {
        return ResponseDTO.<T>builder()
                .code(500)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}





















