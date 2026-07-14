package com.nhnacademy.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateAccountRequest(
        UUID uuid,

        @NotBlank
        @Size(max = 16)
        String name,

        @NotBlank
        @Email
        @Size(max = 32)
        String email,

        @NotBlank
        @Size(max = 256)
        String password
) {}
