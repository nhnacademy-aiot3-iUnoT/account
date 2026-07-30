package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import com.nhnacademy.account.global.error.exception.ConflictException;
import com.nhnacademy.account.global.error.exception.NotFoundException;
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
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            // TODO DB 예외
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

    }

    @Transactional
    public Account createAdminAccount(CreateAccountRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), request.email(), hashedPassword, AccountRole.ADMIN);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            // TODO DB 예외
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public Account findAccount(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account changeAccountStatus(UUID uuid, ChangeAccountStatusRequest request) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.changeStatus(request.action());
        return account;
    }

    @Transactional
    public Account updateAccount(UUID uuid, UpdateAccountRequest request) {

        Optional<Account> currentAccount = accountRepository.findByUuid(uuid);

        if (currentAccount.isEmpty()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account updatedAccount = currentAccount.get();

        if (!updatedAccount.isActive()) {
            throw new ConflictException(ErrorCode.INVALID_ACCOUNT_STATE);
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
    public void withdrawAccount(UUID uuid, WithdrawAccountRequest request) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), account.getHashedPassword())) {
            throw new BadRequestException(ErrorCode.PASSWORD_MISMATCH);
        }

        account.withdraw();
    }

    public boolean availableEmail(EmailAvailabilityRequest request) {
        return !accountRepository.existsByEmail(request.email());
    }

    public boolean availablePassword(UUID uuid, PasswordReuseCheckRequest request) {
        Optional<Account> accountOptional = accountRepository.findByUuid(uuid);
        if (accountOptional.isEmpty()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = accountOptional.get();

        // password is same as current
        if (passwordEncoder.matches(request.newPassword(), account.getHashedPassword())) {
            throw new BadRequestException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        return true;
    }
}
