package com.nhnacademy.account.security;

import com.nhnacademy.account.config.JwtProperties;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.domain.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public String createAccessToken(
            UUID accountUuid,
            AccountRole role,
            AccountStatus status
    ) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(properties.getKeyId())
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(accountUuid.toString())
                .audience(List.copyOf(
                        properties.getIssuedAudiences()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(role.name()))
                .claim("account_status", status.name())
                .build();

        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        );

        return jwt.getTokenValue();
    }

}
