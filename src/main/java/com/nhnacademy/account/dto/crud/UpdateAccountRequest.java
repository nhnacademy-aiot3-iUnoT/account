package com.nhnacademy.account.dto.crud;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 16)
        String name,

        @Size(min = 6, max = 64)
        String password
) {}
