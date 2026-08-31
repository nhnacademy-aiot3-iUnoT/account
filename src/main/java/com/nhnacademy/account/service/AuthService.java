package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.request.RefreshTokenRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.ForbiddenException;
import com.nhnacademy.account.global.error.exception.UnauthorizedException;
import com.nhnacademy.account.global.util.EmailNormalizer;
import com.nhnacademy.account.repository.AccountRepository;
import com.nhnacademy.account.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getHashedPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        validateTokenIssuable(account);

        String accessToken = jwtProvider.createAccessToken(
                account.getUuid(),
                account.getAccountRole(),
                account.getAccountStatus()
        );
        String refreshToken = refreshTokenService.issue(account.getUuid());

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        UUID accountUuid = refreshTokenService.consume(request.refreshToken());
        Account account = accountRepository.findByUuid(accountUuid)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_TOKEN));

        validateTokenIssuable(account);

        String accessToken = jwtProvider.createAccessToken(
                account.getUuid(),
                account.getAccountRole(),
                account.getAccountStatus()
        );
        String refreshToken = refreshTokenService.issue(account.getUuid());

        return new LoginResponse(accessToken, refreshToken);
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private void validateTokenIssuable(Account account) {
        if (account.isWithdrawn()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_WITHDRAWN);
        }

        if (account.isLocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_LOCKED);
        }
    }
}
