package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.global.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ApiResponse<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        if (request == null) {
            return ApiResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        Account account = new Account(request.name(), request.email(), request.password());

        return ApiResponse.success(account);
    }

}
