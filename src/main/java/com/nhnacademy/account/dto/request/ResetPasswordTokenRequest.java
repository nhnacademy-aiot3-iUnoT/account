package com.nhnacademy.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordTokenRequest(
    @NotBlank
    @Email
    @Size(max = 254)
    String email
) {
}
