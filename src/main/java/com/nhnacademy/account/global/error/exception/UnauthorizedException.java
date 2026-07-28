package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
