package com.nhnacademy.account.service;

import com.nhnacademy.account.config.RefreshTokenProperties;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    static final String TOKEN_PREFIX = "auth:refresh:token:";
    static final int SESSION_ID_BYTES = 16;
    static final int SECRET_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;
    private final RefreshTokenProperties properties;

    public String issue(UUID accountUuid) {
        String token = randomHex(SESSION_ID_BYTES)
                + "."
                + randomHex(SECRET_BYTES);

        redisTemplate.opsForValue().set(
                tokenKey(token),
                accountUuid.toString(),
                properties.getTtl()
        );

        return token;
    }

    public UUID consume(String token) {
        String accountUuid = redisTemplate.opsForValue()
                .getAndDelete(tokenKey(token));

        if (accountUuid == null) {
            throw invalidToken();
        }

        try {
            return UUID.fromString(accountUuid);
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    public void revoke(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    static String tokenKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return TOKEN_PREFIX + HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String randomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.INVALID_TOKEN);
    }
}
