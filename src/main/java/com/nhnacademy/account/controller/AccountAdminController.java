package com.nhnacademy.account.controller;

import com.nhnacademy.auth.jwt.AccountUUID;
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
            @AccountUUID UUID requesterUuid
    ) {
        verifyAdmin(requesterUuid);

        List<AccountResponse> accounts = accountService.findAll().stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> viewAllAccounts(
            @AccountUUID UUID requesterUuid,
            @PathVariable UUID uuid
    ) {
        verifyAdmin(requesterUuid);

        AccountResponse accounts = AccountResponse.from(accountService.findAccount(uuid));
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @AccountUUID UUID requesterUuid,
            @Valid @RequestBody CreateAccountRequest createAccountRequest
    ) {
        verifyAdmin(requesterUuid);

        AccountResponse accountResponse = AccountResponse.from(accountService.createAdminAccount(createAccountRequest));
        return ResponseEntity.ok(ApiResponse.success(accountResponse));
    }


    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @AccountUUID UUID requesterUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        verifyAdmin(requesterUuid);

        Account account = accountService.updateAccount(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> changeAccountStatus(
            @AccountUUID UUID requesterUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody ChangeAccountStatusRequest request
    ) {
        verifyAdmin(requesterUuid);

        Account account = accountService.changeAccountStatus(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @AccountUUID UUID requesterUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody WithdrawAccountRequest request
    ) {
        verifyAdmin(requesterUuid);

        accountService.withdrawAccount(uuid, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private void verifyAdmin(UUID requesterUuid) {
        Account requester = accountService.findAccount(requesterUuid);
        if (!requester.isAdmin()) {
            throw new AccessDeniedException();
        }
    }
}
