package com.nhnacademy.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.domain.AccountStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        UUID uuid,
        String name,
        String email,
        AccountRole accountRole,
        AccountStatus accountStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime withdrawnAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getUuid(),
                account.getName(),
                account.getEmail(),
                account.getAccountRole(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getWithdrawnAt()
        );
    }
}
