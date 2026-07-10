package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class AccountInactiveException extends BaseException {

    public AccountInactiveException(ErrorCode errorCode) {
        super(errorCode);
    }
}
