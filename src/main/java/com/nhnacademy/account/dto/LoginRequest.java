package com.nhnacademy.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank
    @Email
    @Size(max = 32)
    String email,

    @NotBlank
    @Size(min = 6, max = 64)
    String password
) { }
