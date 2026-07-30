package com.nhnacademy.account.dto;

public record PasswordReuseCheckResponse(
        boolean available
) {
    public static PasswordReuseCheckResponse from(boolean available) {
        return new PasswordReuseCheckResponse(available);
    }
}
