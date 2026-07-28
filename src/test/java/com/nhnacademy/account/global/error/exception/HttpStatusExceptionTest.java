package com.nhnacademy.account.global.error.exception;

import com.nhnacademy.account.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpStatusExceptionTest {

    @Test
    void usesErrorCodeMessageAsRuntimeExceptionMessage() {
        ConflictException exception =
                new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);

        assertEquals(
                ErrorCode.EMAIL_ALREADY_EXISTS.getMessage(),
                exception.getMessage()
        );
    }

    @Test
    void supportsCustomMessageWithoutChangingErrorCode() {
        BadRequestException exception = new BadRequestException(
                ErrorCode.INVALID_INPUT,
                "이름은 비어 있을 수 없습니다."
        );

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals(
                "이름은 비어 있을 수 없습니다.",
                exception.getMessage()
        );
    }
}
