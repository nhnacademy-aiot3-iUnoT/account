package com.nhnacademy.account.service;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivationTokenService {

    static final String TOKEN_PREFIX = "reactivation:token:";
    static final String ACCOUNT_PREFIX = "reactivation:account:";
    static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;

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
        String storedAccountUuid = redisTemplate.opsForValue()
                .getAndDelete(TOKEN_PREFIX + token);

        if (!accountUuid.toString().equals(storedAccountUuid)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_VERIFICATION_TOKEN
            );
        }

        redisTemplate.delete(ACCOUNT_PREFIX + accountUuid);
    }
}
