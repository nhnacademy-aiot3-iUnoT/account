package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountStatus;
import com.nhnacademy.account.event.MailSendRequestedEvent;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReactivationService {

    private final AccountService accountService;
    private final ReactivationTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontBaseUrl;

    public ReactivationService(
            AccountService accountService,
            ReactivationTokenService tokenService,
            ApplicationEventPublisher eventPublisher,
            @Value("${nhn.server-host}") String frontBaseUrl
    ) {
        this.accountService = accountService;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
        this.frontBaseUrl = frontBaseUrl;
    }

    public void requestVerification(UUID accountUuid) {
        Account account = accountService.findAccount(accountUuid);

        if (account.getAccountStatus() != AccountStatus.INACTIVE) {
            throw new ConflictException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        if (!tokenService.tryAcquireIssueCooldown(accountUuid)) {
            return;
        }

        try {
            String token = tokenService.issue(accountUuid);

            eventPublisher.publishEvent(
                    new MailSendRequestedEvent(
                            account.getEmail(),
                            "계정 재활성화 인증",
                            frontBaseUrl + "/reactivation?token=" + token
                    )
            );
        } catch (RuntimeException exception) {
            releaseIssueCooldown(accountUuid, exception);
            throw exception;
        }
    }

    public Account confirm(UUID accountUuid, String token) {
        tokenService.consume(accountUuid, token);
        return accountService.reactivateAccount(accountUuid);
    }

    private void releaseIssueCooldown(
            UUID accountUuid,
            RuntimeException requestException
    ) {
        try {
            tokenService.releaseIssueCooldown(accountUuid);
        } catch (RuntimeException releaseException) {
            requestException.addSuppressed(releaseException);
        }
    }
}
