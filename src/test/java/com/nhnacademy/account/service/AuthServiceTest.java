package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.request.RefreshTokenRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.UnauthorizedException;
import com.nhnacademy.account.repository.AccountRepository;
import com.nhnacademy.account.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String REFRESH_TOKEN = "a".repeat(32) + "." + "b".repeat(64);
    private static final String ROTATED_REFRESH_TOKEN = "c".repeat(32) + "." + "d".repeat(64);

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginNormalizesEmailWithLocaleRoot() {
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        Account account = new Account("test", "i@test.com", "hashed");
        LoginRequest request = new LoginRequest("  I@Test.COM  ", "password");

        try {
            given(accountRepository.findByEmail("i@test.com"))
                    .willReturn(Optional.of(account));
            given(passwordEncoder.matches(request.password(), account.getHashedPassword()))
                    .willReturn(true);
            given(jwtProvider.createAccessToken(
                    account.getUuid(),
                    account.getAccountRole(),
                    account.getAccountStatus()
            ))
                    .willReturn("access-token");
            given(refreshTokenService.issue(account.getUuid()))
                    .willReturn(REFRESH_TOKEN);

            LoginResponse response = authService.login(request);

            assertEquals("access-token", response.accessToken());
            assertEquals(REFRESH_TOKEN, response.refreshToken());
            then(accountRepository).should().findByEmail("i@test.com");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void refreshConsumesOldTokenAndIssuesNewTokenWithCurrentAccountClaims() {
        Account account = new Account("test", "test@test.com", "hashed");
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        given(refreshTokenService.consume(REFRESH_TOKEN))
                .willReturn(account.getUuid());
        given(accountRepository.findByUuid(account.getUuid()))
                .willReturn(Optional.of(account));
        given(jwtProvider.createAccessToken(
                account.getUuid(),
                account.getAccountRole(),
                account.getAccountStatus()
        )).willReturn("new-access-token");
        given(refreshTokenService.issue(account.getUuid()))
                .willReturn(ROTATED_REFRESH_TOKEN);

        LoginResponse response = authService.refresh(request);

        assertEquals("new-access-token", response.accessToken());
        assertEquals(ROTATED_REFRESH_TOKEN, response.refreshToken());
        then(refreshTokenService).should().consume(REFRESH_TOKEN);
        then(accountRepository).should().findByUuid(account.getUuid());
        then(refreshTokenService).should().issue(account.getUuid());
    }

    @Test
    void refreshRejectsTokenForMissingAccountWithoutIssuingSuccessor() {
        UUID accountUuid = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        given(refreshTokenService.consume(REFRESH_TOKEN)).willReturn(accountUuid);
        given(accountRepository.findByUuid(accountUuid)).willReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(request)
        );

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
        then(refreshTokenService).should().consume(REFRESH_TOKEN);
        then(refreshTokenService).shouldHaveNoMoreInteractions();
    }

    @Test
    void logoutRevokesRefreshTokenIdempotently() {
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);

        authService.logout(request);

        then(refreshTokenService).should().revoke(REFRESH_TOKEN);
    }
}
