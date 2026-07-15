package com.nhnacademy.account.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    // TODO 대칭키 or 비대칭키
    private final PrivateKey jwtPrivateKey;

    public String createAccessToken(UUID accountUuid) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .header()
                    .keyId("account-key-001")
                    .and()
                .issuer("account-api")
                .subject(accountUuid.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(jwtPrivateKey)
                .compact();
    }

}
