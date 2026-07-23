package com.nhnacademy.account.dto.crud;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateAccountRequest(
        @NotNull
        UUID uuid,

        @Size(min = 1, max = 100)
        String name,

        @Size(min = 6, max = 64)
        String password
) {}
