package com.nhnacademy.account.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BaseException;

public class AccountWithdrawnException extends BaseException {

    public AccountWithdrawnException(ErrorCode errorCode) {
        super(errorCode);
    }
}
