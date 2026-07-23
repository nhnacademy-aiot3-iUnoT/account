package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class AccessDeniedException extends BaseException {
    public AccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
