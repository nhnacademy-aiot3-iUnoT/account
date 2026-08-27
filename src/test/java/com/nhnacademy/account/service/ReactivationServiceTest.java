package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.event.MailSendRequestedEvent;
import com.nhnacademy.account.global.error.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ReactivationServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private ReactivationTokenService tokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReactivationService reactivationService;

    @BeforeEach
    void setUp() {
        reactivationService = new ReactivationService(
                accountService,
                tokenService,
                eventPublisher,
                "http://localhost:10404"
        );
    }

    @Test
    void requestVerificationIssuesTokenAndPublishesMailEvent() {
        Account account = inactiveAccount();
        UUID accountUuid = account.getUuid();
        String token = "a".repeat(64);
        given(accountService.findAccount(accountUuid)).willReturn(account);
        given(tokenService.tryAcquireIssueCooldown(accountUuid)).willReturn(true);
        given(tokenService.issue(accountUuid)).willReturn(token);

        reactivationService.requestVerification(accountUuid);

        ArgumentCaptor<MailSendRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(MailSendRequestedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        MailSendRequestedEvent event = eventCaptor.getValue();
        assertEquals(account.getEmail(), event.recipient());
        assertEquals("계정 재활성화 인증", event.subject());
        assertEquals(
                "http://localhost:10404/reactivation?token=" + token,
                event.content()
        );
    }

    @Test
    void requestVerificationDuringCooldownDoesNotIssueTokenOrPublish() {
        Account account = inactiveAccount();
        UUID accountUuid = account.getUuid();
        given(accountService.findAccount(accountUuid)).willReturn(account);
        given(tokenService.tryAcquireIssueCooldown(accountUuid)).willReturn(false);

        reactivationService.requestVerification(accountUuid);

        then(tokenService).should().tryAcquireIssueCooldown(accountUuid);
        then(tokenService).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void requestVerificationFailureReleasesCooldownAndRethrows() {
        Account account = inactiveAccount();
        UUID accountUuid = account.getUuid();
        RuntimeException publishException = new RuntimeException("queue rejected");
        given(accountService.findAccount(accountUuid)).willReturn(account);
        given(tokenService.tryAcquireIssueCooldown(accountUuid)).willReturn(true);
        given(tokenService.issue(accountUuid)).willReturn("a".repeat(64));
        willThrow(publishException)
                .given(eventPublisher)
                .publishEvent(any(MailSendRequestedEvent.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> reactivationService.requestVerification(accountUuid)
        );

        assertSame(publishException, thrown);
        then(tokenService).should().releaseIssueCooldown(accountUuid);
    }

    @Test
    void rejectVerificationRequestForActiveAccount() {
        Account account = activeAccount();
        UUID accountUuid = account.getUuid();
        given(accountService.findAccount(accountUuid)).willReturn(account);

        assertThrows(
                ConflictException.class,
                () -> reactivationService.requestVerification(accountUuid)
        );

        then(tokenService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void confirmConsumesTokenBeforeReactivatingAccount() {
        Account account = inactiveAccount();
        UUID accountUuid = account.getUuid();
        String token = "a".repeat(64);
        given(accountService.reactivateAccount(accountUuid)).willReturn(account);

        Account result = reactivationService.confirm(accountUuid, token);

        assertSame(account, result);
        then(tokenService).should().consume(accountUuid, token);
        then(accountService).should().reactivateAccount(accountUuid);
    }

    private Account inactiveAccount() {
        Account account = activeAccount();
        account.deactivate();
        return account;
    }

    private Account activeAccount() {
        return new Account("test", "test@test.com", "hashed");
    }
}
