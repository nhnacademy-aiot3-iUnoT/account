package com.nhnacademy.account.service;

import com.nhnacademy.account.global.error.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PasswordResetTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new PasswordResetTokenService(
                redisTemplate,
                new SecureRandom()
        );
    }

    @Test
    void acquireIssueCooldownStoresOneMinuteMarkerWhenAbsent() {
        String email = "test@test.com";
        mockValueOperations();
        given(valueOperations.setIfAbsent(
                PasswordResetTokenService.ISSUE_COOLDOWN_PREFIX + email,
                "1",
                PasswordResetTokenService.ISSUE_COOLDOWN_TTL
        )).willReturn(true);

        boolean acquired = tokenService.tryAcquireIssueCooldown(email);

        assertTrue(acquired);
    }

    @Test
    void rejectIssueDuringExistingCooldown() {
        String email = "test@test.com";
        mockValueOperations();
        given(valueOperations.setIfAbsent(
                PasswordResetTokenService.ISSUE_COOLDOWN_PREFIX + email,
                "1",
                PasswordResetTokenService.ISSUE_COOLDOWN_TTL
        )).willReturn(false);

        boolean acquired = tokenService.tryAcquireIssueCooldown(email);

        assertFalse(acquired);
    }

    @Test
    void releaseIssueCooldownDeletesMarker() {
        String email = "test@test.com";

        tokenService.releaseIssueCooldown(email);

        then(redisTemplate).should().delete(
                PasswordResetTokenService.ISSUE_COOLDOWN_PREFIX + email
        );
    }

    @Test
    void issueInvalidatesPreviousTokenAndStoresNewToken() {
        String email = "test@test.com";
        String emailKey = PasswordResetTokenService.EMAIL_PREFIX + email;
        String oldToken = "b".repeat(64);
        mockValueOperations();
        given(valueOperations.get(emailKey)).willReturn(oldToken);

        String token = tokenService.issue(email);

        assertEquals(64, token.length());
        assertTrue(token.matches("[0-9a-f]{64}"));
        then(redisTemplate).should().delete(
                PasswordResetTokenService.TOKEN_PREFIX + oldToken
        );
        then(valueOperations).should().set(
                PasswordResetTokenService.TOKEN_PREFIX + token,
                email,
                PasswordResetTokenService.TOKEN_TTL
        );
        then(valueOperations).should().set(
                emailKey,
                token,
                PasswordResetTokenService.TOKEN_TTL
        );
    }

    @Test
    void consumeReturnsEmailAndDeletesEmailIndex() {
        String email = "test@test.com";
        String token = "a".repeat(64);
        mockValueOperations();
        given(valueOperations.getAndDelete(
                PasswordResetTokenService.TOKEN_PREFIX + token
        )).willReturn(email);

        String result = tokenService.consume(token);

        assertEquals(email, result);
        then(redisTemplate).should().delete(
                PasswordResetTokenService.EMAIL_PREFIX + email
        );
    }

    @Test
    void rejectMissingToken() {
        String token = "a".repeat(64);
        mockValueOperations();
        given(valueOperations.getAndDelete(
                PasswordResetTokenService.TOKEN_PREFIX + token
        )).willReturn(null);

        assertThrows(
                BadRequestException.class,
                () -> tokenService.consume(token)
        );
    }

    private void mockValueOperations() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }
}
