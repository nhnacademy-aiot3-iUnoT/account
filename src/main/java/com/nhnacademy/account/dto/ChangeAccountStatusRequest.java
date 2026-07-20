package com.nhnacademy.account.dto;

import com.nhnacademy.account.domain.AccountStatusAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeAccountStatusRequest(
        @NotNull
        AccountStatusAction action,

        @NotBlank
        String reason
) {
}
