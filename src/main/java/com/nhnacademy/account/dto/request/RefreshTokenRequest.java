package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "^[0-9a-f]{32}\\.[0-9a-f]{64}$")
        String refreshToken
) {
}
