package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
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
    public List<Account> searchAccounts(@RequestParam String email) {

        return accountService.findAllByEmail(email);
    }

    @GetMapping
    public List<Account> searchAccounts(@RequestParam List<String> uuids) {
        return accountService.findByUuids(uuids);
    }
}
