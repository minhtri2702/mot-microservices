package com.mot.exception.handler;

import com.mot.exception.MasterDataIsNotFound;
import com.mot.exception.Model.ApiError;
import com.mot.exception.UnauthorizedException;
import com.mot.exception.UnprocessableEntityException;
import com.mot.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public BaseResponse<ApiError> handleUnprocessableEntity(UnprocessableEntityException ex, WebRequest request) {
        log.error(ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage());
        return new BaseResponse<>(false, apiError, "An error occurred");
    }

    @ExceptionHandler(MasterDataIsNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResponse<ApiError> handleMasterDataIsNotFound(MasterDataIsNotFound ex, WebRequest request) {
        log.error(ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new BaseResponse<>(false, apiError, "Master data is not found");
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public BaseResponse<ApiError> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        log.error(ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return new BaseResponse<>(false, apiError, "Unauthorized");
    }
}
