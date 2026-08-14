package com.nhnacademy.account.dto.request;

import java.util.UUID;

public record SignupCompensateRequest(
        UUID token,
        UUID accountUuid
) {
}
