package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        String currentPassword,

        @NotBlank
        @Size(min = 6, max = 64)
        String newPassword
) {
}
