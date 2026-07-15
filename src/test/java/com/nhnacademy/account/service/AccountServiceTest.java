package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.dto.UpdateAccountRequest;
import com.nhnacademy.account.dto.WithdrawAccountRequest;
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
                .findByEmail(request.email());

        then(accountRepository)
                .should()
                .save(any(Account.class));
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
                UUID.randomUUID(),
                "test",
                "test@test.com",
                "hashed"
        );

        given(accountRepository.findByUuid(any(UUID.class)))
            .willReturn(Optional.of(account));

        Account result = accountService.updateAccount(request);

        assertEquals(account, result);

        then(accountRepository)
                .should(only())
                .findByUuid(any(UUID.class));
    }

    @Test
    void deleteAccount() {
        WithdrawAccountRequest request = new WithdrawAccountRequest(UUID.randomUUID());

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        accountService.deleteAccount(request);

        assertNull(account.getName());
        assertNull(account.getEmail());
        assertNull(account.getHashedPassword());
    }
}