package com.nhnacademy.account.controller;

import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.request.RefreshTokenRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
import com.nhnacademy.account.global.util.ApiResponse;
import com.nhnacademy.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return tokenResponse(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        LoginResponse loginResponse = authService.refresh(refreshTokenRequest);
        return tokenResponse(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        authService.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> tokenResponse(LoginResponse loginResponse) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(loginResponse));
    }
}
