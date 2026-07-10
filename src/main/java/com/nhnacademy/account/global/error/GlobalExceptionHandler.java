package com.nhnacademy.account.global.error;

import com.nhnacademy.account.global.error.exception.BaseException;
import com.nhnacademy.account.global.util.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ApiResponse<?> handleException(BaseException exception) {
        return ApiResponse.error(exception.getErrorCode().getCode(), exception.getMessage());
    }

}
