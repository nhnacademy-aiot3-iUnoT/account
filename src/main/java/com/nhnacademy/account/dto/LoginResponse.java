package com.nhnacademy.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginResponse(

    @NotBlank
    @Size(max = 256)
     String accessToken
){ }
