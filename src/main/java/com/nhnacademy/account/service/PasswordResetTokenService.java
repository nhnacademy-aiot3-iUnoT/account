package com.nhnacademy.account.service;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    static final String TOKEN_PREFIX = "pwd-reset:token:";
    static final String EMAIL_PREFIX = "pwd-reset:email:";
    static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;

    public String issue(String email) {
        String emailKey = EMAIL_PREFIX + email;
        String oldToken = redisTemplate.opsForValue().get(emailKey);

        if (oldToken != null) {
            redisTemplate.delete(TOKEN_PREFIX + oldToken);
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                email,
                TOKEN_TTL
        );
        redisTemplate.opsForValue().set(
                emailKey,
                token,
                TOKEN_TTL
        );

        return token;
    }

    public String consume(String token) {
        String email = redisTemplate.opsForValue()
                .getAndDelete(TOKEN_PREFIX + token);

        if (email == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_VERIFICATION_TOKEN
            );
        }

        redisTemplate.delete(EMAIL_PREFIX + email);
        return email;
    }
}
