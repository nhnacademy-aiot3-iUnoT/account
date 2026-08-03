package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountNameRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        String name
) {
}
