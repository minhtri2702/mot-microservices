package com.mot.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private T data;
    private String message;
    public static <T> BaseResponse<T> ok(T data, String message) {
        return new BaseResponse<>(true, data,message);
    }
    public static <T> BaseResponse<T> error(T data, String message) {
        return new BaseResponse<>(false, data, message);
    }
}
