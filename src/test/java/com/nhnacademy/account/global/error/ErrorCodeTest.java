package com.nhnacademy.account.global.error;

import com.nhnacademy.account.global.util.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodeTest {

    @Test
    void codesAreUnique() {
        long uniqueCodeCount = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        assertEquals(ErrorCode.values().length, uniqueCodeCount);
    }

    @Test
    void apiResponseUsesPublicErrorCodeInsteadOfEnumName() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED);

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), response.error().code());
        assertEquals(
                ErrorCode.UNAUTHORIZED.getMessage(),
                response.error().message()
        );
    }
}
