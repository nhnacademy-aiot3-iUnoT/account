package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.LoginRequest;
import com.nhnacademy.account.dto.LoginResponse;
import com.nhnacademy.account.exception.AccountNotFoundException;
import com.nhnacademy.account.exception.InvalidAccountStateException;
import com.nhnacademy.account.global.error.ErrorCode;
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
                .orElseThrow(() -> new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), account.getHashedPassword())) {
            throw new AccountNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (account.isWithdrawn() || account.isLocked()) {
            throw new InvalidAccountStateException(ErrorCode.INVALID_ACCOUNT_STATE);
        }

        String token = jwtProvider.createAccessToken(account.getUuid());
        return new LoginResponse(token);
    }
}
