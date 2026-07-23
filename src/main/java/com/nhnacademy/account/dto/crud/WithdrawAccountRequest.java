package com.nhnacademy.account.dto.crud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawAccountRequest(
        @NotBlank
        @Size(min = 6, max = 32)
        String password
) {}
