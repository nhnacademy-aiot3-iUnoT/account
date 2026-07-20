package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.AccountResponse;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
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
    public ResponseEntity<ApiResponse<List<AccountResponse>>> viewAllAccounts() {
        // TODO admin 체크

        List<AccountResponse> accounts = accountService.findAll().stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> viewAllAccounts(
            @PathVariable UUID uuid
    ) {
        // TODO admin 체크

        AccountResponse accounts = AccountResponse.from(accountService.findAccount(uuid));
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest
    ) {
        AccountResponse accountResponse = AccountResponse.from(accountService.createAdminAccount(createAccountRequest));
        return ResponseEntity.ok(ApiResponse.success(accountResponse));
    }


    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Account account = accountService.updateAccount(request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> changeAccountStatus(
            @PathVariable UUID uuid,
            @Valid @RequestBody ChangeAccountStatusRequest request
    ) {
        Account account = accountService.changeAccountStatus(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @Valid @RequestBody WithdrawAccountRequest request
    ) {
        accountService.withdrawAccount(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }


}
