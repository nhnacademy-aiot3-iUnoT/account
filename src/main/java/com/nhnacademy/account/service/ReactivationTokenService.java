package com.nhnacademy.account.service;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivationTokenService {

    static final String TOKEN_PREFIX = "reactivation:token:";
    static final String ACCOUNT_PREFIX = "reactivation:account:";
    static final String ISSUE_COOLDOWN_PREFIX = "reactivation:cooldown:";
    static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    static final Duration ISSUE_COOLDOWN_TTL = Duration.ofSeconds(30);
    private static final RedisScript<Long> CONSUME_IF_OWNED_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                        return 0
                    end
                    redis.call('DEL', KEYS[1])
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;

    public boolean tryAcquireIssueCooldown(UUID accountUuid) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                ISSUE_COOLDOWN_PREFIX + accountUuid,
                "1",
                ISSUE_COOLDOWN_TTL
        );

        return Boolean.TRUE.equals(acquired);
    }

    public void releaseIssueCooldown(UUID accountUuid) {
        redisTemplate.delete(ISSUE_COOLDOWN_PREFIX + accountUuid);
    }

    public String issue(UUID accountUuid) {
        String accountKey = ACCOUNT_PREFIX + accountUuid;
        String oldToken = redisTemplate.opsForValue().get(accountKey);

        if (oldToken != null) {
            redisTemplate.delete(TOKEN_PREFIX + oldToken);
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                accountUuid.toString(),
                TOKEN_TTL
        );
        redisTemplate.opsForValue().set(
                accountKey,
                token,
                TOKEN_TTL
        );

        return token;
    }

    public void consume(UUID accountUuid, String token) {
        Long consumed = redisTemplate.execute(
                CONSUME_IF_OWNED_SCRIPT,
                List.of(TOKEN_PREFIX + token),
                accountUuid.toString()
        );

        if (!Long.valueOf(1).equals(consumed)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_VERIFICATION_TOKEN
            );
        }

        redisTemplate.delete(ACCOUNT_PREFIX + accountUuid);
    }
}
