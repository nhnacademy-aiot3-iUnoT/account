package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReactivationConfirmRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{64}$")
        String token
) {
}
