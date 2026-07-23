package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.AccountResponse;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.exception.AccessDeniedException;
import com.nhnacademy.account.global.util.ApiResponse;
import com.nhnacademy.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts")
public class AccountAdminController {
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> viewAllAccounts(
            @RequestHeader("X-USER-ID") String uuidStr
    ) {
        UUID uuid = UUID.fromString(uuidStr);
        Account account = accountService.findAccount(uuid);

        if (!account.isAdmin()) {
            throw new AccessDeniedException();
        }

        List<AccountResponse> accounts = accountService.findAll().stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> viewAllAccounts(
            @RequestHeader("X-USER-ID") String uuidStr,
            @PathVariable UUID uuid
    ) {
        UUID uuidSelf = UUID.fromString(uuidStr);
        Account account = accountService.findAccount(uuidSelf);

        if (!account.isAdmin()) {
            throw new AccessDeniedException();
        }

        AccountResponse accounts = AccountResponse.from(accountService.findAccount(uuid));
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest,
            @RequestHeader("X-USER-ID") String uuidStr
    ) {
        UUID uuidSelf = UUID.fromString(uuidStr);
        Account account = accountService.findAccount(uuidSelf);

        if (!account.isAdmin()) {
            throw new AccessDeniedException();
        }

        AccountResponse accountResponse = AccountResponse.from(accountService.createAdminAccount(createAccountRequest));
        return ResponseEntity.ok(ApiResponse.success(accountResponse));
    }


    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAccountRequest request,
            @RequestHeader("X-USER-ID") String uuidStr
    ) {
        UUID uuidSelf = UUID.fromString(uuidStr);
        Account accountSelf = accountService.findAccount(uuidSelf);

        if (!accountSelf.isAdmin()) {
            throw new AccessDeniedException();
        }

        Account account = accountService.updateAccount(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> changeAccountStatus(
            @PathVariable UUID uuid,
            @Valid @RequestBody ChangeAccountStatusRequest request,
            @RequestHeader("X-USER-ID") String uuidStr
    ) {
        UUID uuidSelf = UUID.fromString(uuidStr);
        Account accountSelf = accountService.findAccount(uuidSelf);

        if (!accountSelf.isAdmin()) {
            throw new AccessDeniedException();
        }


        Account account = accountService.changeAccountStatus(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @PathVariable UUID uuid,
            @Valid @RequestBody WithdrawAccountRequest request,
            @RequestHeader("X-USER-ID") String uuidStr
    ) {
        UUID uuidSelf = UUID.fromString(uuidStr);
        Account account = accountService.findAccount(uuidSelf);

        if (!account.isAdmin()) {
            throw new AccessDeniedException();
        }


        accountService.withdrawAccount(uuid, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }


}
