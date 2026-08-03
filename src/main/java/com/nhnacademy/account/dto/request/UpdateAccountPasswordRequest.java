package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountPasswordRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        String password
) {
}
