package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class InvalidAccountStateException extends BaseException {
    public InvalidAccountStateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
