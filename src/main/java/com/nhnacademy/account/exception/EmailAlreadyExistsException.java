package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
