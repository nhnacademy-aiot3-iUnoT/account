package com.nhnacademy.account.dto.crud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CreateAccountRequest(

        @NotBlank
        @Size(max = 16)
        String name,

        @NotBlank
        @Email
        @Size(max = 32)
        String email,

        @NotBlank
        @Size(min = 6, max = 64)
        String password


) {}
