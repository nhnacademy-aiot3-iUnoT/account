package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.ResetPasswordRequest;
import com.nhnacademy.account.event.MailSendRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private PasswordResetTokenService tokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                accountService,
                tokenService,
                eventPublisher,
                "http://localhost:10404"
        );
    }

    @Test
    void requestNormalizesEmailAndPublishesMailEvent() {
        String rawEmail = " Test@Example.COM ";
        String email = "test@example.com";
        String token = "a".repeat(64);
        given(accountService.existsByEmail(email)).willReturn(true);
        given(tokenService.tryAcquireIssueCooldown(email)).willReturn(true);
        given(tokenService.issue(email)).willReturn(token);

        passwordResetService.request(rawEmail);

        ArgumentCaptor<MailSendRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(MailSendRequestedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        MailSendRequestedEvent event = eventCaptor.getValue();
        assertEquals(email, event.recipient());
        assertEquals("비밀번호 초기화 메일", event.subject());
        assertEquals(
                "http://localhost:10404/pwd/" + token,
                event.content()
        );
    }

    @Test
    void requestDuringCooldownReturnsWithoutIssuingTokenOrPublishing() {
        String email = "test@example.com";
        given(accountService.existsByEmail(email)).willReturn(true);
        given(tokenService.tryAcquireIssueCooldown(email)).willReturn(false);

        passwordResetService.request(email);

        then(tokenService).should().tryAcquireIssueCooldown(email);
        then(tokenService).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void requestFailureReleasesCooldownAndRethrowsOriginalException() {
        String email = "test@example.com";
        RuntimeException publishException = new RuntimeException("queue rejected");
        given(accountService.existsByEmail(email)).willReturn(true);
        given(tokenService.tryAcquireIssueCooldown(email)).willReturn(true);
        given(tokenService.issue(email)).willReturn("a".repeat(64));
        willThrow(publishException)
                .given(eventPublisher)
                .publishEvent(any(MailSendRequestedEvent.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> passwordResetService.request(email)
        );

        assertSame(publishException, thrown);
        then(tokenService).should().releaseIssueCooldown(email);
    }

    @Test
    void requestForUnknownEmailReturnsWithoutPublishing() {
        String email = "unknown@test.com";
        given(accountService.existsByEmail(email)).willReturn(false);

        passwordResetService.request(email);

        then(tokenService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void resetConsumesTokenAndUpdatesPassword() {
        String token = "a".repeat(64);
        String email = "test@test.com";
        Account account = new Account("test", email, "hashed");
        ResetPasswordRequest request =
                new ResetPasswordRequest("new-password");
        given(tokenService.consume(token)).willReturn(email);
        given(accountService.findAccountByEmail(email)).willReturn(account);

        passwordResetService.reset(token, request);

        then(accountService).should()
                .resetPassword(account.getUuid(), request);
    }
}
