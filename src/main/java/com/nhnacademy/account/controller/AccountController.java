package com.nhnacademy.account.controller;

import com.nhnacademy.auth.jwt.AccountUUID;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.AccountResponse;
import com.nhnacademy.account.dto.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.EmailAvailabilityResponse;
import com.nhnacademy.account.dto.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.global.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        Account account = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getCurrentAccount(
            @AccountUUID UUID accountUuid
    ) {
        Account account = accountService.findAccount(accountUuid);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Account account = accountService.updateAccount(request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @Valid @RequestBody WithdrawAccountRequest request
    ) {
        accountService.withdrawAccount(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmail(
            @Valid @RequestBody EmailAvailabilityRequest request
    ) {
        boolean available = accountService.availableEmail(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        EmailAvailabilityResponse.from(available)
                )
        );
    }

    @PostMapping("/pwd")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkPassword(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody PasswordReuseCheckRequest request
    ) {
        boolean available = accountService.availablePassword(accountUuid, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        EmailAvailabilityResponse.from(available)
                )
        );
    }


}
