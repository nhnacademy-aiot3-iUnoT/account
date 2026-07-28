package com.nhnacademy.account.controller;

import com.nhnacademy.auth.jwt.AccountUUID;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.AccountResponse;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.ForbiddenException;
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
@RequestMapping("/api/accounts/admin")
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


    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @AccountUUID UUID requesterUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        verifyAdmin(requesterUuid);

        Account account = accountService.updateAccount(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @PutMapping("/{uuid}/status")
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
        if (!(requester.isAdmin() && requester.isActive())) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }
    }
}
