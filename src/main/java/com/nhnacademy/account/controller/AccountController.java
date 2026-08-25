package com.nhnacademy.account.controller;

import com.nhnacademy.account.dto.request.*;
import com.nhnacademy.account.security.AccountUUID;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.response.AccountResponse;
import com.nhnacademy.account.dto.response.EmailAvailabilityResponse;
import com.nhnacademy.account.dto.response.PasswordReuseCheckResponse;
import com.nhnacademy.account.dto.request.CreateAccountRequest;
import com.nhnacademy.account.dto.request.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.request.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.request.WithdrawAccountRequest;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.global.util.ApiResponse;
import com.nhnacademy.account.service.PasswordResetService;
import com.nhnacademy.account.service.ReactivationService;
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
    private final PasswordResetService passwordResetService;
    private final ReactivationService reactivationService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        accountService.createAccount(request);

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
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountName(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody UpdateAccountNameRequest request
    ) {
        Account account = accountService.updateAccountName(accountUuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PutMapping("/me/pwd")
    public ResponseEntity<ApiResponse<AccountResponse>> changeOwnPassword(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody ChangeOwnPasswordRequest request
    ) {
        Account account = accountService.changeOwnPassword(accountUuid, request);
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

    @PostMapping("/pwd")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody ResetPasswordTokenRequest request
    ) {
        passwordResetService.request(request.email());

        return ResponseEntity.accepted().body(ApiResponse.ok());
    }

    @PostMapping("/pwd/reset/{token}")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable("token") String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.reset(token, request);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/me/reactivation/verification")
    public ResponseEntity<ApiResponse<Void>> requestReactivationVerification(
            @AccountUUID UUID accountUuid
    ) {
        reactivationService.requestVerification(accountUuid);

        return ResponseEntity.accepted().body(ApiResponse.ok());
    }

    @PostMapping("/me/reactivation/confirm")
    public ResponseEntity<ApiResponse<AccountResponse>> confirmReactivation(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody ReactivationConfirmRequest request
    ) {
        Account account = reactivationService.confirm(
                accountUuid,
                request.token()
        );

        return ResponseEntity.ok(
                ApiResponse.success(AccountResponse.from(account))
        );
    }
}
