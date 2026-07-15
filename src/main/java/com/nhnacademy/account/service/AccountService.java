package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.dto.UpdateAccountRequest;
import com.nhnacademy.account.dto.WithdrawAccountRequest;
import com.nhnacademy.account.exception.AccountNotFoundException;
import com.nhnacademy.account.exception.EmailAlreadyExistsException;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), request.email(), hashedPassword);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

       return accountRepository.save(account);
    }

    public Account findAccount(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account updateAccount(UpdateAccountRequest request) {

        Optional<Account> currentAccount = accountRepository.findByUuid(request.uuid());

        if (currentAccount.isEmpty()) {
            throw new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account updatedAccount = currentAccount.get();

        updatedAccount.changeName(request.name());


        String hashedPassword = passwordEncoder.encode(request.password());
        updatedAccount.changeHashedPassword(hashedPassword);

        return updatedAccount;
    }


    @Transactional
    public void deleteAccount(WithdrawAccountRequest request) {
        Account account = accountRepository.findByUuid(request.uuid())
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.withdraw();
    }
}
