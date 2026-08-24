package com.nhnacademy.account.service;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

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
            given(jwtProvider.createAccessToken(account.getUuid(), account.getAccountRole()))
                    .willReturn("access-token");

            LoginResponse response = authService.login(request);

            assertEquals("access-token", response.accessToken());
            then(accountRepository).should().findByEmail("i@test.com");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
