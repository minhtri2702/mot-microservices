package com.demo.commons.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private T data;
    private String message;cd //
    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(true, data, "SUCCESS");
    }
    public static BaseResponse<?> error(String message) {
        return new BaseResponse<>(false, null, message);
    }
}

