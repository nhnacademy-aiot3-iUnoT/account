package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public Account createAccount(Account account) {
        if (accountRepository.findByEmail(account.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (accountRepository.findByUuid(account.getUuid()).isPresent()) {
            throw new IllegalArgumentException("Account already exists");
        }

       return accountRepository.save(account);
    }

    public Account findByUuid(UUID uuid) {
        return accountRepository.findByUuid(uuid).orElse(null);
    }

}
