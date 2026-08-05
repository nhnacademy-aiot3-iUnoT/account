package com.nhnacademy.account.security;

import com.nhnacademy.account.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final PrivateKey jwtPrivateKey;
    private final JwtProperties properties;
    private final Clock clock;

    public String createAccessToken(UUID accountUuid) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
                .header()
                    .keyId(properties.getKeyId())
                    .and()
                .issuer(properties.getIssuer())
                .audience().add(properties.getIssuedAudiences()).and()
                .subject(accountUuid.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(jwtPrivateKey)
                .compact();
    }

}
