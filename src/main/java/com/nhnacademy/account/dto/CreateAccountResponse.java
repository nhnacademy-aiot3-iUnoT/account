package com.nhnacademy.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountResponse(
        @NotBlank
        @Size(max = 255)
        String token
) {}
