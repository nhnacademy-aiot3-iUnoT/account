package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.ForbiddenException;
import com.nhnacademy.account.global.error.exception.UnauthorizedException;
import com.nhnacademy.account.repository.AccountRepository;
import com.nhnacademy.account.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getHashedPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (account.isWithdrawn()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_WITHDRAWN);
        }

        if (account.isLocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_LOCKED);
        }


        String token = jwtProvider.createAccessToken(account.getUuid());
        return new LoginResponse(token);
    }
}
