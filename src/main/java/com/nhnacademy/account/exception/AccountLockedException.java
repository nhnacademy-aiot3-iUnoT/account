package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class AccountLockedException extends BaseException {
    public AccountLockedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
