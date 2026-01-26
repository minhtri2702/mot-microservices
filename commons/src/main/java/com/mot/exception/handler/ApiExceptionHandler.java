package com.mot.exception.handler;

import com.mot.exception.MasterDataIsNotFound;
import com.mot.exception.Model.ApiError;
import com.mot.exception.UnauthorizedException;
import com.mot.exception.UnprocessableEntityException;
import com.mot.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public BaseResponse handleUnprocessableEntity(UnprocessableEntityException ex, WebRequest request) {
        log.error(ex.getMessage());
        HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
        ApiError apiError = new ApiError(httpStatus.value() , ex.getMessage());
        return new BaseResponse(false, apiError, "An error occurred");
    }
    @ExceptionHandler(MasterDataIsNotFound.class)
    @ResponseStatus(HttpStatus.FOUND)
    public BaseResponse handleMasterDataIsNotFound(MasterDataIsNotFound ex, WebRequest request) {
        log.error(ex.getMessage());
        HttpStatus httpStatus = HttpStatus.FOUND;
        ApiError apiError = new ApiError(httpStatus.value() , ex.getMessage());
        return new BaseResponse(false, apiError, "Master data is not found");
    }
    @ExceptionHandler(MasterDataIsNotFound.class)
    @ResponseStatus(HttpStatus.FOUND)
    public BaseResponse handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        log.error(ex.getMessage());
        HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
        ApiError apiError = new ApiError(httpStatus.value() , ex.getMessage());
        return new BaseResponse(false, apiError, "Unauthorized");
    }
}
