package com.nhnacademy.account.security;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    // TODO 대칭키 or 비대칭키

    public String createAccessToken(UUID accountUuid) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .issuer("account-api")
                .subject(accountUuid.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .compact();
    }

}
