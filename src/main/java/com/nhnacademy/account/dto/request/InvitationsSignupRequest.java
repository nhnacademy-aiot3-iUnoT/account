package com.nhnacademy.account.dto.request;

import java.util.UUID;

public record InvitationsSignupRequest(
        UUID token,
        String email,
        UUID accountUuid
) {
}
