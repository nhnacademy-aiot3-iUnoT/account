package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.domain.AccountStatus;
import com.nhnacademy.account.dto.request.*;
import com.nhnacademy.account.dto.response.InternalAccountInfoResponse;
import com.nhnacademy.account.global.client.InvitationClient;
import com.nhnacademy.account.global.client.OrganizationClient;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import com.nhnacademy.account.global.error.exception.ConflictException;
import com.nhnacademy.account.global.error.exception.NotFoundException;
import com.nhnacademy.account.global.util.EmailNormalizer;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationClient invitationClient;
    private final OrganizationClient organizationClient;

    @Transactional
    public void createAccount(CreateAccountRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), email, hashedPassword);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        InvitationsSignupRequest invitationRequest = new InvitationsSignupRequest(
                request.inviteToken(),
                email,
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
        String email = EmailNormalizer.normalize(request.email());
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(request.name(), email, hashedPassword, AccountRole.ADMIN);

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
        return accountRepository.existsByEmail(EmailNormalizer.normalize(email));
    }

    public Account findAccount(UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public List<Account> findAllByEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        if (normalizedEmail.contains("@")) {
            Account account = accountRepository
                    .findByEmailAndAccountStatusNot(
                            normalizedEmail,
                            AccountStatus.WITHDRAWN
                    )
                    .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

            return List.of(account);
        }

        List<Account> accounts = accountRepository
                .findAllByEmailStartingWithAndAccountStatusNot(
                        normalizedEmail.concat("@"),
                        AccountStatus.WITHDRAWN
                );
        if (accounts.isEmpty()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        return accounts;
    }

    public List<Account> findByUuids(List<String> uuids) {
        if (uuids.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueUuids = new LinkedHashSet<>();
        for (String uuid : uuids) {
            try {
                uniqueUuids.add(UUID.fromString(uuid.trim()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(ErrorCode.INVALID_INPUT);
            }
        }
        List<UUID> uuidList = List.copyOf(uniqueUuids);

        List<Account> accounts = new ArrayList<>(
                accountRepository.findAccountsByUuidIsInAndAccountStatusNot(
                        uuidList,
                        AccountStatus.WITHDRAWN
                )
        );
        accounts.sort(Comparator.comparingInt(
                account -> uuidList.indexOf(account.getUuid())
        ));

        return accounts;
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
    public Account reactivateAccount(UUID uuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.activate();
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
    public Account changeOwnPassword(UUID uuid, ChangeOwnPasswordRequest request) {
        Account account = findActiveAccount(uuid);

        if (!passwordEncoder.matches(request.currentPassword(), account.getHashedPassword())) {
            throw new BadRequestException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), account.getHashedPassword())) {
            throw new BadRequestException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        account.changeHashedPassword(passwordEncoder.encode(request.newPassword()));
        return account;
    }

    @Transactional
    public Account resetPasswordByAdmin(UUID uuid, AdminResetPasswordRequest request) {
        return replacePassword(uuid, request.password());
    }

    @Transactional
    public Account resetPassword(UUID uuid, ResetPasswordRequest request) {
        return replacePassword(uuid, request.password());
    }

    private Account replacePassword(UUID uuid, String password) {
        Account account = findActiveAccount(uuid);
        account.changeHashedPassword(passwordEncoder.encode(password));
        return account;
    }

    private Account findActiveAccount(UUID uuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.isActive()) {
            throw new ConflictException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        return account;
    }



    @Transactional
    public void withdrawAccount(UUID uuid, WithdrawAccountRequest request) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), account.getHashedPassword())) {
            throw new BadRequestException(ErrorCode.PASSWORD_MISMATCH);
        }

        LeaveOrgRequest leaveOrgRequest = new LeaveOrgRequest(
                account.getUuid(),
                account.getEmail()
        );
        organizationClient.leaveOrganization(leaveOrgRequest);

        account.withdraw();
    }

    @Transactional
    public InternalAccountInfoResponse withdrawAccount(UUID uuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        InternalAccountInfoResponse response = InternalAccountInfoResponse.from(account);

        account.withdraw();

        return response;
    }

    @Transactional
    public void withdrawAccountBulk(List<UUID> uuids) {
        for (UUID uuid : uuids) {
            Account account = accountRepository.findByUuid(uuid)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
            account.withdraw();
        }
    }

    public boolean availableEmail(EmailAvailabilityRequest request) {
        return !accountRepository.existsByEmail(EmailNormalizer.normalize(request.email()));
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
