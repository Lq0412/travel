package com.lq.travel.common;

import com.lq.travel.exception.ErrorCode;

/**
 * 统一响应体构建工具类
 * 
 * @deprecated 已废弃，请使用 {@link com.lq.travel.common.ResponseUtils} 替代
 */
@Deprecated(since = "2025-11-03", forRemoval = true)
public class ResultUtils {
    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @return
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败（带泛型）
     *
     * @param code
     * @param message
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> BaseResponse<T> error(int code, String message, Class<T> clazz) {
        return (BaseResponse<T>) new BaseResponse<>(code, null, message);
    }

}
