package com.nhnacademy.account.dto.response;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.domain.AccountStatus;

import java.time.LocalDateTime;
import java.util.UUID;

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
        boolean withdrawn = account.isWithdrawn();

        return new AccountResponse(
                account.getUuid(),
                withdrawn ? null : account.getName(),
                withdrawn ? null : account.getEmail(),
                account.getAccountRole(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getWithdrawnAt()
        );
    }
}
