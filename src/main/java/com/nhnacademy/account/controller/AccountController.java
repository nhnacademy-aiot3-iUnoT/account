package com.nhnacademy.account.controller;

import com.nhnacademy.account.dto.request.*;
import com.nhnacademy.account.event.MailSendRequestedEvent;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.NotFoundException;
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
import com.nhnacademy.account.service.ReactivationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;


@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final ReactivationService reactivationService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${nhn.server-host}")
    private String BASE_URL;

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
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountPassword(
            @AccountUUID UUID accountUuid,
            @Valid @RequestBody UpdateAccountPasswordRequest request
    ) {
        Account account = accountService.updateAccountPassword(accountUuid, request);
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
    public ResponseEntity<?> passwordResetToken(
            @Valid @RequestBody ResetPasswordTokenRequest request,
            SecureRandom random
    ) {
        String email = request.email().trim().toLowerCase();
        String emailKey = "pwd-reset:email:" + email;
        String tokenPrefix = "pwd-reset:token:";

        if (!accountService.existsByEmail(email)) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }


        String oldToken = redisTemplate.opsForValue().get(emailKey);
        if (oldToken != null) {
            redisTemplate.delete("pwd-reset:token:" + oldToken);
        }

        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        Duration ttl = Duration.ofMinutes(5);

        redisTemplate.opsForValue().set(
                tokenPrefix + token,
                email,
                ttl
        );
        redisTemplate.opsForValue().set(emailKey, token, ttl);

        eventPublisher.publishEvent(
                new MailSendRequestedEvent(
                        email,
                        "비밀번호 초기화 메일",
                        BASE_URL + "/pwd/" + token
                )
        );

        return ResponseEntity.accepted().body(ApiResponse.ok());
    }

    @PostMapping("/pwd/reset/{token}")
    public ResponseEntity<?> passwordChange(
            @PathVariable("token") String token,
            @Valid @RequestBody UpdateAccountPasswordRequest request
    ) {
        String emailPrefix = "pwd-reset:email:";
        String tokenKey = "pwd-reset:token:" + token;

        String email = redisTemplate.opsForValue().getAndDelete(tokenKey);
        if (email == null) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = accountService.findAccountByEmail(email);

        accountService.updateAccountPassword(account.getUuid(), request);

        redisTemplate.delete(emailPrefix + email);

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
