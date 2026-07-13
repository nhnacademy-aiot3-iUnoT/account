package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.exception.EmailAlreadyExistsException;
import com.nhnacademy.account.global.error.ErrorCode;
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
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

       return accountRepository.save(account);
    }

    public Account findByUuid(UUID uuid) {
        return accountRepository.findByUuid(uuid).orElse(null);
    }

}
