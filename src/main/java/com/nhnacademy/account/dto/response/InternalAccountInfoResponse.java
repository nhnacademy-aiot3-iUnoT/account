package com.nhnacademy.account.dto.response;

import com.nhnacademy.account.domain.Account;

import java.util.UUID;

public record InternalAccountInfoResponse(
        UUID accountUuid,
        String name,
        String email
) {
    public static InternalAccountInfoResponse from(Account account) {
        return new InternalAccountInfoResponse(account.getUuid(), account.getName(), account.getEmail());
    }
}
