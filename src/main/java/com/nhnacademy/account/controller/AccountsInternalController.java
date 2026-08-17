package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.InternalAccountInfoResponse;
import com.nhnacademy.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/internal")
@RequiredArgsConstructor
public class AccountsInternalController {
    private final AccountService accountService;

    @GetMapping("/search")
    public List<InternalAccountInfoResponse> searchAccounts(@RequestParam String email) {

        List<Account> accounts = accountService.findAllByEmail(email);

        return accounts.stream().map(InternalAccountInfoResponse::from).toList();
    }

    @GetMapping
    public List<InternalAccountInfoResponse> searchAccounts(@RequestParam List<String> uuids) {

        List<Account> accounts = accountService.findByUuids(uuids);

        return accounts.stream().map(InternalAccountInfoResponse::from).toList();
    }
}
