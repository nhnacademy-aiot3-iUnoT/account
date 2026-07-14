package com.nhnacademy.account.service;

import com.nhnacademy.account.dto.LoginRequest;
import com.nhnacademy.account.dto.LoginResponse;
import com.nhnacademy.account.security.AccountPrincipal;
import com.nhnacademy.account.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                request.email(),
                                request.password()
                        )
                );

        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();

        String token = jwtProvider.createAccessToken(principal.getUuid());
        return new LoginResponse(token);
    }
}
