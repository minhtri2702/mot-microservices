package com.mot.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private boolean success;
    private T data;
    private String message;

    public BaseResponse() {}

    public BaseResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static <T> BaseResponse<T> ok(T data) {
        BaseResponse<T> r = new BaseResponse<>();
        r.success = true;
        r.data = data;
        r.message = "SUCCESS";
        return r;
    }

    @SuppressWarnings("unchecked")
    public static <T> BaseResponse<T> ok() {
        BaseResponse<T> r = new BaseResponse<>();
        r.success = true;
        r.message = "SUCCESS";
        return r;
    }

    public static BaseResponse<?> error(String message) {
        BaseResponse<?> r = new BaseResponse<>();
        r.success = false;
        r.message = message;
        return r;
    }
}
