package com.nhnacademy.account.dto.request;

import java.util.UUID;

public record CreateOrganizationWithMemberRequest(
        String token,
        String email,
        UUID uuid
) {
}
