package com.nhnacademy.account.controller;

import com.nhnacademy.account.security.AccountUUID;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.CreateAccountRequest;
import com.nhnacademy.account.dto.request.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.request.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.request.UpdateAccountRequest;
import com.nhnacademy.account.dto.request.WithdrawAccountRequest;
import com.nhnacademy.account.dto.response.AccountResponse;
import com.nhnacademy.account.dto.response.EmailAvailabilityResponse;
import com.nhnacademy.account.dto.response.PasswordReuseCheckResponse;
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

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Account account = accountService.updateAccount(accountUuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody WithdrawAccountRequest request
    ) {
        accountService.withdrawAccount(accountUuid, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmail(
            @Valid @RequestBody EmailAvailabilityRequest request
    ) {
        boolean available = accountService.availableEmail(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        new EmailAvailabilityResponse(available)
                )
        );
    }

    @PostMapping("/check-pwd")
    public ResponseEntity<ApiResponse<PasswordReuseCheckResponse>> checkPassword(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody PasswordReuseCheckRequest request
    ) {
        boolean available = accountService.availablePassword(accountUuid, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        new PasswordReuseCheckResponse(available)
                )
        );
    }


}
