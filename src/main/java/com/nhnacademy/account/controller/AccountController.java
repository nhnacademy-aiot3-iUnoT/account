package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.dto.UpdateAccountRequest;
import com.nhnacademy.account.dto.WithdrawAccountRequest;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.global.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<?> viewAllAccounts() {
        List<Account> accounts = accountService.findAll();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAccount() {
        Account account = accountService.findAccount(/* TODO */ null);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/me")
    public ResponseEntity<?> updateAccount(@Valid @RequestBody UpdateAccountRequest request) {
        Account account = accountService.updateAccount(request);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@Valid @RequestBody WithdrawAccountRequest request) {
        accountService.deleteAccount(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
