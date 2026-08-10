package com.nhnacademy.account.dto.response;

public record CreateAccountResponse(
        Boolean success,
        Boolean isOwner
) {
}
