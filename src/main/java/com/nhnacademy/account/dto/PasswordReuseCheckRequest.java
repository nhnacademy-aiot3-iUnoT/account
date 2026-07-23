package com.nhnacademy.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordReuseCheckRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        String newPassword
) {
}
