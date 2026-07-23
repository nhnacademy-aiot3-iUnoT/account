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
import com.nhnacademy.account.exception.*;
import com.nhnacademy.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        assertThrows(EmailAlreadyExistsException.class,
                () -> accountService.createAccount(request));


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

        assertThrows(EmailAlreadyExistsException.class,
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

        assertThrows(AccountNotFoundException.class,
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

        assertThrows(AccountNotFoundException.class,
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

        assertThrows(InvalidAccountStateException.class,
                () -> accountService.updateAccount(uuid, request));
    }



    @Test
    void withdrawAccount() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("hashed");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

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

        assertThrows(InvalidInputException.class,
                () -> accountService.withdrawAccount(uuid, request));
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

        assertTrue(accountService.availablePassword(UUID.randomUUID(), request));
    }

    @Test
    void availablePasswordNotFoundAccount() {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("test");

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.availablePassword(UUID.randomUUID(), request));
    }

    @Test
    void availablePasswordSameAsCurrentPassword() {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("hashed");

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        assertThrows(SameAsCurrentPasswordException.class,
                () -> accountService.availablePassword(UUID.randomUUID(), request));
    }

}
