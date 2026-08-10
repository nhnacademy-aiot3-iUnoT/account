package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;

public class UpstreamServiceException extends BaseException {

    public UpstreamServiceException(String message) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message, cause);
    }
}
