package com.nhnacademy.account.service;

import com.nhnacademy.account.config.RefreshTokenProperties;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration TOKEN_TTL = Duration.ofDays(14);
    private static final String REFRESH_TOKEN = "a".repeat(32) + "." + "b".repeat(64);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService tokenService;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setTtl(TOKEN_TTL);
        tokenService = new RefreshTokenService(
                redisTemplate,
                new SecureRandom(),
                properties
        );
    }

    @Test
    void issueStoresOnlyTokenHashWithConfiguredTtl() {
        UUID accountUuid = UUID.randomUUID();
        mockValueOperations();

        String token = tokenService.issue(accountUuid);

        assertTrue(token.matches("^[0-9a-f]{32}\\.[0-9a-f]{64}$"));
        String tokenKey = RefreshTokenService.tokenKey(token);
        assertFalse(tokenKey.contains(token));
        then(valueOperations).should().set(
                tokenKey,
                accountUuid.toString(),
                TOKEN_TTL
        );
    }

    @Test
    void consumeAtomicallyDeletesTokenAndReturnsAccountUuid() {
        UUID accountUuid = UUID.randomUUID();
        mockValueOperations();
        given(valueOperations.getAndDelete(
                RefreshTokenService.tokenKey(REFRESH_TOKEN)
        )).willReturn(accountUuid.toString());

        UUID result = tokenService.consume(REFRESH_TOKEN);

        assertEquals(accountUuid, result);
    }

    @Test
    void consumedTokenCannotBeUsedAgain() {
        UUID accountUuid = UUID.randomUUID();
        mockValueOperations();
        given(valueOperations.getAndDelete(
                RefreshTokenService.tokenKey(REFRESH_TOKEN)
        )).willReturn(accountUuid.toString(), (String) null);

        assertEquals(accountUuid, tokenService.consume(REFRESH_TOKEN));
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tokenService.consume(REFRESH_TOKEN)
        );

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void rejectMissingTokenAsInvalidAuthenticationToken() {
        mockValueOperations();
        given(valueOperations.getAndDelete(
                RefreshTokenService.tokenKey(REFRESH_TOKEN)
        )).willReturn(null);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tokenService.consume(REFRESH_TOKEN)
        );

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void revokeDeletesHashedTokenKeyWithoutRequiringTokenToExist() {
        tokenService.revoke(REFRESH_TOKEN);

        then(redisTemplate).should().delete(
                RefreshTokenService.tokenKey(REFRESH_TOKEN)
        );
    }

    private void mockValueOperations() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }
}
