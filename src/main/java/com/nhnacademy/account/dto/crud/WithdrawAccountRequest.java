package com.nhnacademy.account.dto.crud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record WithdrawAccountRequest(
        @NotNull
        UUID uuid,

        @NotBlank
        @Size(min = 6, max = 64)
        String password
) {}
