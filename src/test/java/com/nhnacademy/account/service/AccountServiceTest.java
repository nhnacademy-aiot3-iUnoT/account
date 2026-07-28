package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountStatusAction;
import com.nhnacademy.account.dto.AccountResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account account;


    @BeforeEach
    void setUp() {
        account = new Account("test", "test@test.com", "hashed");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void createAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "test",
                "test@test.com",
                "hashed"
        );


        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");

        given(accountRepository.save(any(Account.class)))
                .willReturn(account);

        Account result = accountService.createAccount(request);

        assertEquals(account, result);

        then(accountRepository)
                .should()
                .existsByEmail(request.email());

        then(accountRepository)
                .should()
                .save(any(Account.class));
    }

    @Test
    void createAccountWithExistingEmail() {
        CreateAccountRequest request = new CreateAccountRequest(
                "test",
                "test@test.com",
                "hashed"
        );

        given(accountRepository.existsByEmail(request.email()))
                .willReturn(true);

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> accountService.createAccount(request)
        );

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());

    }

    @Test
    void createAdminAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "test",
                "test@test.com",
                "hashed"
        );


        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");

        given(accountRepository.save(any(Account.class)))
                .willReturn(account);

        Account result = accountService.createAdminAccount(request);

        assertEquals(account, result);

        then(accountRepository)
                .should()
                .existsByEmail(request.email());

        then(accountRepository)
                .should()
                .save(any(Account.class));
    }

    @Test
    void createAdminAccountWithExistingEmail() {
        CreateAccountRequest request = new CreateAccountRequest(
                "test",
                "test@test.com",
                "hashed"
        );

        given(accountRepository.existsByEmail(request.email()))
                .willReturn(true);

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");

        assertThrows(ConflictException.class,
                () -> accountService.createAdminAccount(request));


    }

    @Test
    void changeAccountStatus() {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request = new ChangeAccountStatusRequest(AccountStatusAction.DEACTIVATE, uuid.toString());

        Account before = account;
        Account after = new Account(account.getName(), account.getEmail(), account.getHashedPassword(), account.getAccountRole());
        after.changeStatus(AccountStatusAction.DEACTIVATE);

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(before));

        Account acc = accountService.changeAccountStatus(uuid, request);

        assertEquals(after.getAccountStatus(), acc.getAccountStatus());
    }

    @Test
    void changeAccountStatusWithNotFoundUuid() {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request = new ChangeAccountStatusRequest(AccountStatusAction.DEACTIVATE, uuid.toString());

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.changeAccountStatus(uuid, request));
    }

    @Test
    void findAccount() {
        given(accountRepository.findByUuid(any(UUID.class)))
            .willReturn(Optional.of(account));

        Account result = accountService.findAccount(account.getUuid());

        assertEquals(account, result);

        then(accountRepository)
                .should()
                .findByUuid(any(UUID.class));
    }

    @Test
    void findAll() {
        List<Account> accountList = List.of(account, account);

        given(accountRepository.findAll())
                .willReturn(accountList);

        List<Account> result = accountService.findAll();
        assertEquals(accountList, result);

        then(accountRepository)
                .should(only())
                .findAll();

    }

    @Test
    void updateAccount() {
        UpdateAccountRequest request = new UpdateAccountRequest(
                "test",
                "hashed"
        );
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
            .willReturn(Optional.of(account));

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");

        Account result = accountService.updateAccount(uuid, request);

        assertEquals(account, result);

        then(accountRepository)
                .should(only())
                .findByUuid(any(UUID.class));
    }

    @Test
    void updateAccountWithNotFoundUuid() {
        UpdateAccountRequest request = new UpdateAccountRequest(
                "test",
                "hashed"
        );
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.updateAccount(uuid, request));
    }

    @Test
    void updateAccountWithInvalidState() {
        UpdateAccountRequest request = new UpdateAccountRequest(
                "test",
                "hashed"
        );
        UUID uuid = UUID.randomUUID();

        Account notActive = new Account(account.getName(), account.getEmail(), account.getHashedPassword(), account.getAccountRole());
        notActive.changeStatus(AccountStatusAction.DEACTIVATE);

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(notActive));

        assertThrows(ConflictException.class,
                () -> accountService.updateAccount(uuid, request));
    }



    @Test
    void withdrawAccount() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("password");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                .willReturn(true);

        accountService.withdrawAccount(uuid, request);

        assertNull(account.getName());
        assertNull(account.getEmail());
        assertNull(account.getHashedPassword());
    }

    @Test
    void withdrawAccountWithWrongPassword() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("wrong-password");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                .willReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.withdrawAccount(uuid, request)
        );

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void availableEmailTrueTest() {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("test@test.com");

        given(accountRepository.existsByEmail(any(String.class)))
                .willReturn(true);

        assertTrue(accountService.availableEmail(request));

    }

    @Test
    void availableEmailFalseTest() {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("test@test.com");

        given(accountRepository.existsByEmail(any(String.class)))
                .willReturn(false);

        assertFalse(accountService.availableEmail(request));

    }

    @Test
    void availablePasswordTrueTest() {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("password");

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.newPassword(), account.getHashedPassword()))
                .willReturn(false);

        assertTrue(accountService.availablePassword(UUID.randomUUID(), request));
    }

    @Test
    void availablePasswordNotFoundAccount() {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("test");

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.availablePassword(UUID.randomUUID(), request));
    }

    @Test
    void availablePasswordSameAsCurrentPassword() {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("hashed");

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.newPassword(), account.getHashedPassword()))
                .willReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.availablePassword(UUID.randomUUID(), request)
        );

        assertEquals(ErrorCode.SAME_AS_CURRENT_PASSWORD, exception.getErrorCode());
    }

}
