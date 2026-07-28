package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;

public class NotFoundException extends BaseException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
