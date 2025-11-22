/*
 * File: .\src\main\java\com\lq\common\exception\GlobalExceptionHandler.java
 * Description: 全局异常处理器 - 统一处理各类异常
 * Created: 2025-10-17
 * Updated: 2025-11-03
 */

package com.lq.travel.exception;


import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，提供友好的错误提示
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.lq.common.controller")
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseDTO<?> businessExceptionHandler(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理 - @Valid 注解校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errorMessage);
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "参数校验失败: " + errorMessage);
    }

    /**
     * 参数绑定异常处理 - 表单提交校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseDTO<?> bindExceptionHandler(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", errorMessage);
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "参数绑定失败: " + errorMessage);
    }

    /**
     * 约束违反异常处理 - @Validated 注解校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseDTO<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反: {}", errorMessage);
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "参数校验失败: " + errorMessage);
    }

    /**
     * 缺少必需参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseDTO<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("缺少必需参数: {}, 类型: {}", e.getParameterName(), e.getParameterType());
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, 
                String.format("缺少必需参数: %s (%s)", e.getParameterName(), e.getParameterType()));
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseDTO<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: 参数名={}, 期望类型={}, 实际值={}", 
                e.getName(), e.getRequiredType(), e.getValue());
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, 
                String.format("参数类型错误: %s 应为 %s 类型", e.getName(), 
                        e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知"));
    }

    /**
     * JSON解析异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseDTO<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("HTTP消息不可读: {}", e.getMessage());
        String message = "请求参数格式错误，请检查JSON格式";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("JSON parse error")) {
                message = "JSON解析失败，请检查数据格式";
            } else if (e.getMessage().contains("Required request body is missing")) {
                message = "请求体不能为空";
            }
        }
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 请求方法不支持异常处理
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseDTO<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}, 支持的方法: {}", e.getMethod(), e.getSupportedHttpMethods());
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, 
                String.format("不支持的请求方法: %s，请使用: %s", e.getMethod(), e.getSupportedHttpMethods()));
    }

    /**
     * 文件上传大小超限异常处理
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseDTO<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String maxSizeStr = maxSize > 0 ? formatFileSize(maxSize) : "未知";
        log.warn("文件上传大小超限: 最大允许 {}", maxSizeStr);
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, 
                String.format("文件大小超过限制，最大允许: %s", maxSizeStr));
    }

    /**
     * 空指针异常处理
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseDTO<?> nullPointerExceptionHandler(NullPointerException e) {
        log.error("空指针异常", e);
        // 生产环境不暴露详细错误信息
        return ResponseUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后重试");
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseDTO<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "参数错误: " + e.getMessage());
    }

    /**
     * 通用运行时异常处理 - 兜底处理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseDTO<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("运行时异常", e);
        // 生产环境不暴露详细错误堆栈
        return ResponseUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后重试");
    }

    /**
     * 通用异常处理 - 最终兜底
     */
    @ExceptionHandler(Exception.class)
    public ResponseDTO<?> exceptionHandler(Exception e) {
        log.error("未知异常", e);
        return ResponseUtils.error(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}

