package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.request.*;
import com.nhnacademy.account.global.client.InvitationClient;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationClient invitationClient;

    @Transactional
    public void createAccount(CreateAccountRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), request.email().toLowerCase(), hashedPassword);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        InvitationsSignupRequest invitationRequest = new InvitationsSignupRequest(
                request.inviteToken(),
                request.email(),
                account.getUuid()
        );
        invitationClient.signup(invitationRequest);

        try {
            accountRepository.saveAndFlush(account);
        } catch (RuntimeException saveException) {
            SignupCompensateRequest compensateRequest = new SignupCompensateRequest(
                    request.inviteToken(),
                    account.getUuid()
            );

            try {
                invitationClient.compensate(compensateRequest);
            } catch (RuntimeException compensateException) {
                saveException.addSuppressed(compensateException);
            }

            throw saveException;
        }
    }

    @Transactional
    public Account createAdminAccount(CreateAdminAccountRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), request.email().toLowerCase(), hashedPassword, AccountRole.ADMIN);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    public Account findAccount(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAllByEmail(String email) {
        String emailAddress = email.contains("@") ? email.toLowerCase() : email.concat("@").toLowerCase();

        List<Account> accounts = accountRepository.findAllByEmailStartingWith(emailAddress);
        if (accounts.isEmpty()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        return accounts;
    }

    public List<Account> findByUuids(List<String> uuids) {
        if (uuids.isEmpty()) {
            return List.of();
        }

        List<UUID> uuidList = new ArrayList<>();
        for (String uuid : uuids) {
            try {
                uuidList.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
        }

        return accountRepository.findAccountsByUuidIsIn(uuidList);
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
    public Account updateAccountName(UUID uuid, UpdateAccountNameRequest request) {

        Optional<Account> currentAccount = accountRepository.findByUuid(uuid);

        if (currentAccount.isEmpty()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account updatedAccount = currentAccount.get();

        if (!updatedAccount.isActive()) {
            throw new ConflictException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        updatedAccount.changeName(request.name());
        return updatedAccount;
    }

    @Transactional
    public Account updateAccountPassword(UUID uuid, UpdateAccountPasswordRequest request) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.isActive()) {
            throw new ConflictException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        account.changeHashedPassword(hashedPassword);
        return account;
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
