package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.ResetPasswordRequest;
import com.nhnacademy.account.event.MailSendRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PasswordResetService {

    private final AccountService accountService;
    private final PasswordResetTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontBaseUrl;

    public PasswordResetService(
            AccountService accountService,
            PasswordResetTokenService tokenService,
            ApplicationEventPublisher eventPublisher,
            @Value("${nhn.server-host}") String frontBaseUrl
    ) {
        this.accountService = accountService;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
        this.frontBaseUrl = frontBaseUrl;
    }

    public void request(String rawEmail) {
        String email = normalizeEmail(rawEmail);

        if (!accountService.existsByEmail(email)) {
            return;
        }

        if (!tokenService.tryAcquireIssueCooldown(email)) {
            return;
        }

        try {
            String token = tokenService.issue(email);

            eventPublisher.publishEvent(
                    new MailSendRequestedEvent(
                            email,
                            "비밀번호 초기화 메일",
                            frontBaseUrl + "/pwd/" + token
                    )
            );
        } catch (RuntimeException exception) {
            releaseIssueCooldown(email, exception);
            throw exception;
        }
    }

    public void reset(
            String token,
            ResetPasswordRequest request
    ) {
        String email = tokenService.consume(token);
        Account account = accountService.findAccountByEmail(email);

        accountService.resetPassword(account.getUuid(), request);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void releaseIssueCooldown(
            String email,
            RuntimeException requestException
    ) {
        try {
            tokenService.releaseIssueCooldown(email);
        } catch (RuntimeException releaseException) {
            requestException.addSuppressed(releaseException);
        }
    }
}
