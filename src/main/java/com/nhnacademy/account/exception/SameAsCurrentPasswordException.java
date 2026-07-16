package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class SameAsCurrentPasswordException extends BaseException {

    public SameAsCurrentPasswordException(ErrorCode errorCode) {
        super(errorCode);
    }
}
