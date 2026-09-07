package com.nhnacademy.account.service;

import com.nhnacademy.account.global.error.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReactivationTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReactivationTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new ReactivationTokenService(
                redisTemplate,
                new SecureRandom()
        );
    }

    @Test
    void acquireIssueCooldownStoresThirtySecondMarkerWhenAbsent() {
        UUID accountUuid = UUID.randomUUID();
        mockValueOperations();
        given(valueOperations.setIfAbsent(
                ReactivationTokenService.ISSUE_COOLDOWN_PREFIX + accountUuid,
                "1",
                ReactivationTokenService.ISSUE_COOLDOWN_TTL
        )).willReturn(true);

        boolean acquired = tokenService.tryAcquireIssueCooldown(accountUuid);

        assertTrue(acquired);
    }

    @Test
    void rejectIssueDuringExistingCooldown() {
        UUID accountUuid = UUID.randomUUID();
        mockValueOperations();
        given(valueOperations.setIfAbsent(
                ReactivationTokenService.ISSUE_COOLDOWN_PREFIX + accountUuid,
                "1",
                ReactivationTokenService.ISSUE_COOLDOWN_TTL
        )).willReturn(false);

        boolean acquired = tokenService.tryAcquireIssueCooldown(accountUuid);

        assertFalse(acquired);
    }

    @Test
    void releaseIssueCooldownDeletesMarker() {
        UUID accountUuid = UUID.randomUUID();

        tokenService.releaseIssueCooldown(accountUuid);

        then(redisTemplate).should().delete(
                ReactivationTokenService.ISSUE_COOLDOWN_PREFIX + accountUuid
        );
    }

    @Test
    void issueInvalidatesPreviousTokenAndStoresNewToken() {
        UUID accountUuid = UUID.randomUUID();
        String accountKey = ReactivationTokenService.ACCOUNT_PREFIX + accountUuid;
        String oldToken = "b".repeat(64);
        mockValueOperations();
        given(valueOperations.get(accountKey)).willReturn(oldToken);

        String token = tokenService.issue(accountUuid);

        assertEquals(64, token.length());
        assertTrue(token.matches("[0-9a-f]{64}"));
        then(redisTemplate).should().delete(
                ReactivationTokenService.TOKEN_PREFIX + oldToken
        );
        then(valueOperations).should().set(
                ReactivationTokenService.TOKEN_PREFIX + token,
                accountUuid.toString(),
                ReactivationTokenService.TOKEN_TTL
        );
        then(valueOperations).should().set(
                accountKey,
                token,
                ReactivationTokenService.TOKEN_TTL
        );
    }

    @Test
    void consumeDeletesSingleUseTokenAndAccountIndex() {
        UUID accountUuid = UUID.randomUUID();
        String token = "a".repeat(64);
        given(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.eq(List.of(
                        ReactivationTokenService.TOKEN_PREFIX + token
                )),
                org.mockito.ArgumentMatchers.eq(accountUuid.toString())
        )).willReturn(1L);

        tokenService.consume(accountUuid, token);

        then(redisTemplate).should().delete(
                ReactivationTokenService.ACCOUNT_PREFIX + accountUuid
        );
    }

    @Test
    void rejectMissingToken() {
        UUID accountUuid = UUID.randomUUID();
        String token = "a".repeat(64);

        assertThrows(
                BadRequestException.class,
                () -> tokenService.consume(accountUuid, token)
        );
    }

    @Test
    void rejectTokenIssuedForAnotherAccount() {
        UUID accountUuid = UUID.randomUUID();
        String token = "a".repeat(64);
        given(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.eq(List.of(
                        ReactivationTokenService.TOKEN_PREFIX + token
                )),
                org.mockito.ArgumentMatchers.eq(accountUuid.toString())
        )).willReturn(0L);

        assertThrows(
                BadRequestException.class,
                () -> tokenService.consume(accountUuid, token)
        );

        then(redisTemplate).should(never()).delete(
                ReactivationTokenService.ACCOUNT_PREFIX + accountUuid
        );
    }

    private void mockValueOperations() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }
}
