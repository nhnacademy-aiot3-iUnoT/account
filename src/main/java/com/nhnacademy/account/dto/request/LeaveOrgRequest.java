package com.nhnacademy.account.dto.request;

import java.util.UUID;

public record LeaveOrgRequest(
        UUID accountUuid,
        String previousEmail
) {
}
