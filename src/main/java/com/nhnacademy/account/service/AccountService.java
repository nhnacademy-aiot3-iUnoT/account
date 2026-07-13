package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.exception.AccountNotFoundException;
import com.nhnacademy.account.exception.EmailAlreadyExistsException;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Account account) {
        if (accountRepository.findByEmail(account.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

       return accountRepository.save(account);
    }

    public Account findByUuid(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }
}
