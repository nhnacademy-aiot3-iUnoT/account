package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.exception.*;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            // TODO DB 예외
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

    }

    @Transactional
    public Account createAdminAccount(CreateAccountRequest request) {
        Account account = new Account(request.name(), request.email(), request.password(), AccountRole.ADMIN);
        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            // TODO DB 예외
            throw new EmailAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public Account findAccount(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account changeAccountStatus(UUID uuid, ChangeAccountStatusRequest request) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.changeStatus(request.action());
        return account;
    }

    @Transactional
    public Account updateAccount(UpdateAccountRequest request) {

        Optional<Account> currentAccount = accountRepository.findByUuid(request.uuid());

        if (currentAccount.isEmpty()) {
            throw new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account updatedAccount = currentAccount.get();

        updatedAccount.changeName(request.name());


        if (!updatedAccount.isActive()) {
            throw new InvalidAccountStateException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        if (request.name() != null) {
            updatedAccount.changeName(request.name());
        }


        if (request.password() != null) {
            String hashedPassword = passwordEncoder.encode(request.password());
            updatedAccount.changeHashedPassword(hashedPassword);
        }
        return updatedAccount;
    }



    @Transactional
    public void withdrawAccount(WithdrawAccountRequest request) {
        Account account = accountRepository.findByUuid(request.uuid())
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getHashedPassword().equals(request.password())) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT);
        }

        account.withdraw();
    }

    public boolean availableEmail(EmailAvailabilityRequest request) {
        return accountRepository.existsByEmail(request.email());
    }

    public boolean availablePassword(UUID uuid, PasswordReuseCheckRequest request) {
        Optional<Account> accountOptional = accountRepository.findByUuid(uuid);
        if (accountOptional.isEmpty()) {
            throw new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = accountOptional.get();

        String requestPasswordHash = passwordEncoder.encode(request.newPassword());

        // password is same as current
        if (account.getHashedPassword().equals(requestPasswordHash)) {
            throw new SameAsCurrentPasswordException(ErrorCode.INVALID_INPUT);
        }

        return true;
    }
}
