package com.nhnacademy.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateAccountRequest(
        UUID uuid,

        @NotBlank
        @Size(max = 16)
        String name,

        @NotBlank
        @Size(max = 32)
        String email,

        @NotBlank
        @Size(min = 6, max = 64)
        String hashedPassword
) {}
