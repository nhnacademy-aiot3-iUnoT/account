package com.nhnacademy.account.dto.request;

import com.nhnacademy.account.domain.Account;

import java.util.UUID;

public record InternalAccountInfoResponse(
        UUID accountUuid,
        String email
) {
    public static InternalAccountInfoResponse from(Account account) {
        return new InternalAccountInfoResponse(account.getUuid(), account.getEmail());
    }
}
