package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.dto.CreateAccountResponse;
import com.nhnacademy.account.exception.InvalidInputException;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.global.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

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
        if (request == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT);
        }

        Account account = new Account(request.name(), request.email(), request.password());
         accountService.createAccount(account);

        CreateAccountResponse createAccountResponse = new CreateAccountResponse("");


        return ResponseEntity.ok(ApiResponse.success(createAccountResponse));
    }



}
