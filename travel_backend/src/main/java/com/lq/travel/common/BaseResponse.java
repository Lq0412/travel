package com.lq.travel.common;


import com.lq.travel.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 * 
 * @deprecated 已废弃，请使用 {@link com.lq.travel.AI.core.model.dto.ResponseDTO} 替代
 * @param <T>
 */
@Deprecated(since = "2025-11-03", forRemoval = true)
@Data
public class BaseResponse<T> implements Serializable {

    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
