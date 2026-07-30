package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawAccountRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        String password
) {}
