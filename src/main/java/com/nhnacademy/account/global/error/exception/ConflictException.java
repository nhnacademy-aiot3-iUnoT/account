package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;

public class ConflictException extends BaseException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
