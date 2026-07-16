package com.nhnacademy.account.dto;

public record PasswordReuseCheckResponse(
        boolean sameAsCurrent
) {
}
