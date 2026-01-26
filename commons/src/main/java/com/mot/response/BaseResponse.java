package com.mot.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class BaseResponse<T> {
    private boolean success;
    private T data;
    private String message;
    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(true, data, "SUCCESS");
    }
    public static <T> BaseResponse<T> ok(T data, String message) {
        return new BaseResponse<>(true, data,message);
    }
    public static BaseResponse<?> error(String message) {
        return new BaseResponse<>(false, null, message);
    }
}

