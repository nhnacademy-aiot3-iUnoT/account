package com.nhnacademy.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailAvailabilityRequest(
        @Email
        @NotBlank
        @Size(max = 32)
        String email
) {
}
