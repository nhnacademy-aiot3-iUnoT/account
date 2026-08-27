package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountStatus;
import com.nhnacademy.account.domain.AccountStatusAction;
import com.nhnacademy.account.dto.request.AdminResetPasswordRequest;
import com.nhnacademy.account.dto.request.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.request.ChangeOwnPasswordRequest;
import com.nhnacademy.account.dto.request.CreateAdminAccountRequest;
import com.nhnacademy.account.dto.request.CreateAccountRequest;
import com.nhnacademy.account.dto.request.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.request.InvitationsSignupRequest;
import com.nhnacademy.account.dto.request.LeaveOrgRequest;
import com.nhnacademy.account.dto.request.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.request.ResetPasswordRequest;
import com.nhnacademy.account.dto.request.SignupCompensateRequest;
import com.nhnacademy.account.dto.request.UpdateAccountNameRequest;
import com.nhnacademy.account.dto.request.WithdrawAccountRequest;
import com.nhnacademy.account.dto.response.InternalAccountInfoResponse;
import com.nhnacademy.account.global.client.InvitationClient;
import com.nhnacademy.account.global.client.OrganizationClient;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import com.nhnacademy.account.global.error.exception.ConflictException;
import com.nhnacademy.account.global.error.exception.NotFoundException;
import com.nhnacademy.account.global.error.exception.UpstreamServiceException;
import com.nhnacademy.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InvitationClient invitationClient;

    @Mock
    private OrganizationClient organizationClient;

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
        UUID inviteToken = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(
                inviteToken,
                "test",
                "test@test.com",
                "hashed"
        );


        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);

        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");


        given(accountRepository.saveAndFlush(any(Account.class)))
                .willReturn(account);

        accountService.createAccount(request);


        then(accountRepository)
                .should()
                .existsByEmail(request.email());

        then(accountRepository)
                .should()
                .saveAndFlush(any(Account.class));

        then(invitationClient)
                .should()
                .signup(any());
    }

    @Test
    void createAccountUsesNormalizedEmailForPersistenceAndInvitation() {
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        CreateAccountRequest request = new CreateAccountRequest(
                UUID.randomUUID(),
                "test",
                "  I@Test.COM  ",
                "password"
        );

        try {
            given(accountRepository.existsByEmail("i@test.com"))
                    .willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("hashed");

            accountService.createAccount(request);

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            ArgumentCaptor<InvitationsSignupRequest> invitationCaptor =
                    ArgumentCaptor.forClass(InvitationsSignupRequest.class);
            then(accountRepository).should().saveAndFlush(accountCaptor.capture());
            then(invitationClient).should().signup(invitationCaptor.capture());
            assertEquals("i@test.com", accountCaptor.getValue().getEmail());
            assertEquals("i@test.com", invitationCaptor.getValue().email());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void createAccountWithExistingEmail() {
        CreateAccountRequest request = new CreateAccountRequest(
                UUID.randomUUID(),
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

        then(invitationClient)
                .shouldHaveNoInteractions();

        then(accountRepository)
                .should(never())
                .saveAndFlush(any(Account.class));

    }

    @Test
    void createAccountCompensatesInvitationWhenAccountSaveFails() {
        UUID inviteToken = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(
                inviteToken,
                "test",
                "test@test.com",
                "hashed"
        );

        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");
        given(accountRepository.saveAndFlush(any(Account.class)))
                .willThrow(new DataIntegrityViolationException("duplicate email"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> accountService.createAccount(request)
        );

        ArgumentCaptor<InvitationsSignupRequest> signupCaptor =
                ArgumentCaptor.forClass(InvitationsSignupRequest.class);
        ArgumentCaptor<SignupCompensateRequest> compensateCaptor =
                ArgumentCaptor.forClass(SignupCompensateRequest.class);

        then(invitationClient)
                .should()
                .signup(signupCaptor.capture());
        then(invitationClient)
                .should()
                .compensate(compensateCaptor.capture());

        assertEquals(inviteToken, compensateCaptor.getValue().token());
        assertEquals(
                signupCaptor.getValue().accountUuid(),
                compensateCaptor.getValue().accountUuid()
        );
    }

    @Test
    void createAccountCompensatesInvitationsFail() {
        UUID inviteToken = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(
                inviteToken,
                "test",
                "test@test.com",
                "hashed"
        );
        SignupCompensateRequest compensateRequest = new SignupCompensateRequest(inviteToken, UUID.randomUUID());

        DataIntegrityViolationException saveException =
                new DataIntegrityViolationException("duplicate email");
        UpstreamServiceException compensateException =
                new UpstreamServiceException("compensation failed");


        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");
        given(accountRepository.saveAndFlush(any(Account.class)))
                .willThrow(saveException);

        willThrow(compensateException)
                .given(invitationClient)
                .compensate(any(SignupCompensateRequest.class));

        DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> accountService.createAccount(request)
        );

        assertSame(saveException, result);
        assertEquals(1, result.getSuppressed().length);
        assertSame(compensateException, result.getSuppressed()[0]);
    }

    @Test
    void createAccountDoesNotSaveWhenInvitationFails() {
        CreateAccountRequest request = new CreateAccountRequest(
                UUID.randomUUID(),
                "test",
                "test@test.com",
                "hashed"
        );
        UpstreamServiceException invitationException =
                new UpstreamServiceException("invitation unavailable");

        given(accountRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");
        willThrow(invitationException)
                .given(invitationClient)
                .signup(any());

        UpstreamServiceException result = assertThrows(
                UpstreamServiceException.class,
                () -> accountService.createAccount(request)
        );

        assertSame(invitationException, result);
        then(accountRepository)
                .should(never())
                .saveAndFlush(any(Account.class));
        then(invitationClient)
                .should(never())
                .compensate(any());

    }

    @Test
    void createAdminAccount() {
        CreateAdminAccountRequest request = new CreateAdminAccountRequest(
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
        CreateAdminAccountRequest request = new CreateAdminAccountRequest(
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
    void createAdminAccountNormalizesEmail() {
        CreateAdminAccountRequest request = new CreateAdminAccountRequest(
                "admin",
                "  ADMIN@Test.COM  ",
                "password"
        );

        given(accountRepository.existsByEmail("admin@test.com"))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("hashed");
        given(accountRepository.save(any(Account.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.createAdminAccount(request);

        assertEquals("admin@test.com", result.getEmail());
        then(accountRepository).should().existsByEmail("admin@test.com");
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
    void reactivateInactiveAccount() {
        UUID uuid = account.getUuid();
        account.deactivate();
        given(accountRepository.findByUuid(uuid)).willReturn(Optional.of(account));

        Account result = accountService.reactivateAccount(uuid);

        assertEquals(AccountStatus.ACTIVE, result.getAccountStatus());
        then(accountRepository).should().findByUuid(uuid);
    }

    @Test
    void reactivateAccountWithInvalidState() {
        UUID uuid = account.getUuid();
        given(accountRepository.findByUuid(uuid)).willReturn(Optional.of(account));

        assertThrows(ConflictException.class,
                () -> accountService.reactivateAccount(uuid));
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
    void emailLookupMethodsNormalizeEmail() {
        given(accountRepository.existsByEmail("test@test.com"))
                .willReturn(true);
        given(accountRepository.findByEmail("test@test.com"))
                .willReturn(Optional.of(account));

        assertTrue(accountService.existsByEmail("  TEST@Test.COM  "));
        assertSame(account, accountService.findAccountByEmail("  TEST@Test.COM  "));

        then(accountRepository).should().existsByEmail("test@test.com");
        then(accountRepository).should().findByEmail("test@test.com");
    }

    @Test
    void findAllByEmailNormalizesWithLocaleRoot() {
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        String email = "  I@Test.COM  ";
        List<Account> accounts = List.of(account);

        try {
            given(accountRepository.findByEmailAndAccountStatusNot(
                    "i@test.com",
                    AccountStatus.WITHDRAWN
            )).willReturn(Optional.of(account));

            List<Account> result = accountService.findAllByEmail(email);

            assertEquals(accounts, result);
            then(accountRepository)
                    .should()
                    .findByEmailAndAccountStatusNot(
                            "i@test.com",
                            AccountStatus.WITHDRAWN
                    );
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void findAllByFullEmailWithNoMatch() {
        given(accountRepository.findByEmailAndAccountStatusNot(
                "missing@test.com",
                AccountStatus.WITHDRAWN
        )).willReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> accountService.findAllByEmail("missing@test.com")
        );

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        then(accountRepository)
                .should()
                .findByEmailAndAccountStatusNot(
                        "missing@test.com",
                        AccountStatus.WITHDRAWN
                );
    }

    @Test
    void findAllByEmailWithLocalPart() {
        String email = "Test";
        List<Account> accounts = List.of(account);

        given(accountRepository.findAllByEmailStartingWithAndAccountStatusNot(
                "test@",
                AccountStatus.WITHDRAWN
        ))
                .willReturn(accounts);

        List<Account> result = accountService.findAllByEmail(email);

        assertSame(accounts, result);
        then(accountRepository)
                .should()
                .findAllByEmailStartingWithAndAccountStatusNot(
                        "test@",
                        AccountStatus.WITHDRAWN
                );
    }

    @Test
    void findAllByEmailWithNoMatches() {
        given(accountRepository.findAllByEmailStartingWithAndAccountStatusNot(
                "missing@",
                AccountStatus.WITHDRAWN
        ))
                .willReturn(List.of());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> accountService.findAllByEmail("missing")
        );

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        then(accountRepository)
                .should()
                .findAllByEmailStartingWithAndAccountStatusNot(
                        "missing@",
                        AccountStatus.WITHDRAWN
                );
    }

    @Test
    void findByUuidsPreservesRequestOrderAndRemovesDuplicates() {
        Account firstAccount = new Account("first", "first@test.com", "hashed");
        Account secondAccount = new Account("second", "second@test.com", "hashed");
        UUID missingUuid = UUID.randomUUID();
        List<UUID> uuidList = List.of(
                secondAccount.getUuid(),
                missingUuid,
                firstAccount.getUuid()
        );
        List<String> uuids = List.of(
                secondAccount.getUuid().toString(),
                missingUuid.toString(),
                firstAccount.getUuid().toString(),
                secondAccount.getUuid().toString()
        );

        given(accountRepository.findAccountsByUuidIsInAndAccountStatusNot(
                uuidList,
                AccountStatus.WITHDRAWN
        )).willReturn(List.of(firstAccount, secondAccount));

        List<Account> result = accountService.findByUuids(uuids);

        assertEquals(List.of(secondAccount, firstAccount), result);
        then(accountRepository)
                .should()
                .findAccountsByUuidIsInAndAccountStatusNot(
                        uuidList,
                        AccountStatus.WITHDRAWN
                );
    }

    @Test
    void findByUuidsWithEmptyList() {
        List<Account> result = accountService.findByUuids(List.of());

        assertTrue(result.isEmpty());
        then(accountRepository).shouldHaveNoInteractions();
    }

    @Test
    void findByUuidsWithInvalidUuid() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.findByUuids(List.of("invalid-uuid"))
        );

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        then(accountRepository).shouldHaveNoInteractions();
    }

    @Test
    void findByUuidsTrimsWhitespace() {
        Account firstAccount = new Account("first", "first@test.com", "hashed");
        Account secondAccount = new Account("second", "second@test.com", "hashed");
        List<UUID> uuidList = List.of(
                firstAccount.getUuid(),
                secondAccount.getUuid()
        );
        List<String> uuids = List.of(
                firstAccount.getUuid().toString(),
                " " + secondAccount.getUuid()
        );

        given(accountRepository.findAccountsByUuidIsInAndAccountStatusNot(
                uuidList,
                AccountStatus.WITHDRAWN
        )).willReturn(List.of(firstAccount, secondAccount));

        List<Account> result = accountService.findByUuids(uuids);

        assertEquals(List.of(firstAccount, secondAccount), result);
        then(accountRepository)
                .should()
                .findAccountsByUuidIsInAndAccountStatusNot(
                        uuidList,
                        AccountStatus.WITHDRAWN
                );
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
    void updateAccountName() {
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("updated");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
            .willReturn(Optional.of(account));

        Account result = accountService.updateAccountName(uuid, request);

        assertEquals(account, result);
        assertEquals("updated", result.getName());

        then(accountRepository)
                .should(only())
                .findByUuid(any(UUID.class));
    }

    @Test
    void updateAccountNameWithNotFoundUuid() {
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("test");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.updateAccountName(uuid, request));
    }

    @Test
    void updateAccountNameWithInvalidState() {
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("test");
        UUID uuid = UUID.randomUUID();

        Account notActive = new Account(account.getName(), account.getEmail(), account.getHashedPassword(), account.getAccountRole());
        notActive.changeStatus(AccountStatusAction.DEACTIVATE);

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(notActive));

        assertThrows(ConflictException.class,
                () -> accountService.updateAccountName(uuid, request));
    }

    @Test
    void changeOwnPassword() {
        ChangeOwnPasswordRequest request = new ChangeOwnPasswordRequest(
                "current-password",
                "new-password"
        );
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(uuid))
                .willReturn(Optional.of(account));
        given(passwordEncoder.matches(request.currentPassword(), account.getHashedPassword()))
                .willReturn(true);
        given(passwordEncoder.matches(request.newPassword(), account.getHashedPassword()))
                .willReturn(false);
        given(passwordEncoder.encode(request.newPassword()))
                .willReturn("new-hashed-password");

        Account result = accountService.changeOwnPassword(uuid, request);

        assertEquals(account, result);
        assertEquals("new-hashed-password", result.getHashedPassword());
        then(accountRepository).should().findByUuid(uuid);
        then(passwordEncoder).should().encode(request.newPassword());
    }

    @Test
    void changeOwnPasswordWithWrongCurrentPassword() {
        ChangeOwnPasswordRequest request = new ChangeOwnPasswordRequest(
                "wrong-password",
                "new-password"
        );
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(uuid))
                .willReturn(Optional.of(account));
        given(passwordEncoder.matches(request.currentPassword(), account.getHashedPassword()))
                .willReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.changeOwnPassword(uuid, request)
        );

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        assertEquals("hashed", account.getHashedPassword());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void changeOwnPasswordWithSamePassword() {
        ChangeOwnPasswordRequest request = new ChangeOwnPasswordRequest(
                "current-password",
                "current-password"
        );
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(uuid))
                .willReturn(Optional.of(account));
        given(passwordEncoder.matches(request.currentPassword(), account.getHashedPassword()))
                .willReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.changeOwnPassword(uuid, request)
        );

        assertEquals(ErrorCode.SAME_AS_CURRENT_PASSWORD, exception.getErrorCode());
        assertEquals("hashed", account.getHashedPassword());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void resetPasswordByAdmin() {
        AdminResetPasswordRequest request = new AdminResetPasswordRequest("new-password");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(uuid))
                .willReturn(Optional.of(account));
        given(passwordEncoder.encode(request.password()))
                .willReturn("new-hashed-password");

        Account result = accountService.resetPasswordByAdmin(uuid, request);

        assertEquals(account, result);
        assertEquals("new-hashed-password", result.getHashedPassword());
    }

    @Test
    void resetPasswordWithToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("new-password");
        UUID uuid = UUID.randomUUID();

        given(accountRepository.findByUuid(uuid))
                .willReturn(Optional.of(account));
        given(passwordEncoder.encode(request.password()))
                .willReturn("new-hashed-password");

        Account result = accountService.resetPassword(uuid, request);

        assertEquals(account, result);
        assertEquals("new-hashed-password", result.getHashedPassword());
    }

    @Test
    void withdrawAccount() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("password");
        UUID uuid = account.getUuid();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                .willReturn(true);
        willAnswer(invocation -> {
            assertFalse(account.isWithdrawn());
            return null;
        }).given(organizationClient).leaveOrganization(any(LeaveOrgRequest.class));

        accountService.withdrawAccount(uuid, request);

        then(organizationClient).should().leaveOrganization(
                new LeaveOrgRequest(uuid, "test@test.com")
        );
        assertTrue(account.isWithdrawn());
        assertNull(account.getEmail());
        assertNull(account.getHashedPassword());
        assertEquals("test", account.getName());
    }

    @Test
    void withdrawAccountWithWrongPassword() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("wrong-password");
        UUID uuid = account.getUuid();

        given(accountRepository.findByUuid(any(UUID.class)))
                .willReturn(Optional.of(account));

        given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                .willReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> accountService.withdrawAccount(uuid, request)
        );

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        assertEquals("test@test.com", account.getEmail());
        assertEquals("hashed", account.getHashedPassword());
        then(organizationClient).shouldHaveNoInteractions();
    }

    @Test
    void withdrawAccountKeepsAccountWhenInventoryCallFails() {
        WithdrawAccountRequest request = new WithdrawAccountRequest("password");
        UUID uuid = account.getUuid();
        UpstreamServiceException inventoryException =
                new UpstreamServiceException("inventory unavailable");

        given(accountRepository.findByUuid(uuid)).willReturn(Optional.of(account));
        given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                .willReturn(true);
        willThrow(inventoryException).given(organizationClient).leaveOrganization(
                new LeaveOrgRequest(uuid, "test@test.com")
        );

        UpstreamServiceException result = assertThrows(
                UpstreamServiceException.class,
                () -> accountService.withdrawAccount(uuid, request)
        );

        assertSame(inventoryException, result);
        assertFalse(account.isWithdrawn());
        assertEquals("test@test.com", account.getEmail());
        assertEquals("hashed", account.getHashedPassword());
    }

    @Test
    void withdrawAccountInternally() {
        UUID uuid = account.getUuid();
        String email = account.getEmail();
        given(accountRepository.findByUuid(uuid)).willReturn(Optional.of(account));

        InternalAccountInfoResponse result = accountService.withdrawAccount(uuid);

        assertEquals(uuid, result.accountUuid());
        assertEquals(email, result.email());
        assertTrue(account.isWithdrawn());
        assertNull(account.getEmail());
        assertNull(account.getHashedPassword());
        then(accountRepository).should().findByUuid(uuid);
    }

    @Test
    void withdrawAccountInternallyWithUnknownUuid() {
        UUID uuid = UUID.randomUUID();
        given(accountRepository.findByUuid(uuid)).willReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> accountService.withdrawAccount(uuid)
        );

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        then(accountRepository).should().findByUuid(uuid);
    }

    @Test
    void withdrawAccountsInBulk() {
        Account firstAccount = new Account("first", "first@test.com", "hashed");
        Account secondAccount = new Account("second", "second@test.com", "hashed");
        List<UUID> uuids = List.of(firstAccount.getUuid(), secondAccount.getUuid());
        given(accountRepository.findByUuid(firstAccount.getUuid()))
                .willReturn(Optional.of(firstAccount));
        given(accountRepository.findByUuid(secondAccount.getUuid()))
                .willReturn(Optional.of(secondAccount));

        accountService.withdrawAccountBulk(uuids);

        assertTrue(firstAccount.isWithdrawn());
        assertTrue(secondAccount.isWithdrawn());
        then(accountRepository).should().findByUuid(firstAccount.getUuid());
        then(accountRepository).should().findByUuid(secondAccount.getUuid());
    }

    @Test
    void withdrawAccountsInBulkWithEmptyList() {
        accountService.withdrawAccountBulk(List.of());

        then(accountRepository).shouldHaveNoInteractions();
    }

    @Test
    void availableEmailTrueTest() {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("test@test.com");

        given(accountRepository.existsByEmail(any(String.class)))
                .willReturn(false);

        assertTrue(accountService.availableEmail(request));

    }

    @Test
    void availableEmailFalseTest() {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("  TEST@Test.COM  ");

        given(accountRepository.existsByEmail("test@test.com"))
                .willReturn(true);

        assertFalse(accountService.availableEmail(request));

        then(accountRepository).should().existsByEmail("test@test.com");

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
