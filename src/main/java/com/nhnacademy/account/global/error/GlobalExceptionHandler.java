package com.nhnacademy.account.global.error;

import com.nhnacademy.account.global.error.exception.BaseException;
import com.nhnacademy.account.global.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<?>> handleException(
            BaseException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn(
                "event=business_error exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={} message={}",
                exception.getClass().getSimpleName(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ApiResponse.error(
                                errorCode.getCode(),
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        List<FieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        log.warn(
                "event=validation_error exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={} fields={}",
                exception.getClass().getSimpleName(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getMethod(),
                request.getRequestURI(),
                fieldErrors.stream().map(FieldError::field).toList()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        log.warn(
                "event=message_not_readable exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={}",
                exception.getClass().getSimpleName(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getMethod(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserDetailsException(
            UsernameNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.ACCOUNT_NOT_FOUND;

        log.warn(
                "event=business_error exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={}",
                exception.getClass().getSimpleName(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getMethod(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error(
                "event=unexpected_error exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={}",
                exception.getClass().getSimpleName(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

}
