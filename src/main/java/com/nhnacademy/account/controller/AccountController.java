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
import org.springframework.web.servlet.function.EntityResponse;

@RestController("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public EntityResponse<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        if (request == null) {
            return EntityResponse.fromObject(ApiResponse.error(ErrorCode.ACCOUNT_NOT_FOUND)).build();
        }

        Account account = new Account(request.name(), request.email(), request.password());

        return EntityResponse.fromObject(ApiResponse.success(account)).build();
    }



}
