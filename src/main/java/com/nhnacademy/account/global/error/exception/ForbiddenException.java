package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
