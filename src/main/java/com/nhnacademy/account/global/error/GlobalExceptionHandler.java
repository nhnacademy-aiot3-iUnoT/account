package com.nhnacademy.account.global.error;

import com.nhnacademy.account.global.error.exception.BaseException;
import com.nhnacademy.account.global.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<?>> handleException(BaseException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(
                        new ApiResponse<>(
                                false,
                                null,
                                new ErrorDetail(
                                        e.getErrorCode().getCode(),
                                        e.getMessage()
                                ),
                                LocalDateTime.now()
                        )
                );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserDetailsException(UsernameNotFoundException e) {
        return ResponseEntity
                .status(ErrorCode.ACCOUNT_NOT_FOUND.getStatus())
                .body(
                        new ApiResponse<>(
                                false,
                                null,
                                new ErrorDetail(
                                        ErrorCode.ACCOUNT_NOT_FOUND.getCode(),
                                        ErrorCode.ACCOUNT_NOT_FOUND.getMessage()
                                ),
                                LocalDateTime.now()
                        )
                );
    }

}
