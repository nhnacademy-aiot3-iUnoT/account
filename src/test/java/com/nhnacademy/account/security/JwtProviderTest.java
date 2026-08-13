package com.nhnacademy.account.security;

import com.nhnacademy.account.config.JwtKeyConfig;
import com.nhnacademy.account.config.JwtProperties;
import com.nhnacademy.account.config.SecurityConfig;
import com.nhnacademy.account.domain.AccountRole;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtProviderTest {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(17);

    private JwtProperties properties;
    private JwtProvider jwtProvider;
    private JwtDecoder jwtDecoder;
    private Instant issuedAt;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        properties = new JwtProperties();
        properties.setIssuer("test-issuer");
        properties.setAudiences(new LinkedHashSet<>(Set.of("account-api")));
        properties.setIssuedAudiences(
                new LinkedHashSet<>(Set.of("account-api", "inventory-api"))
        );
        properties.setAccessTokenTtl(ACCESS_TOKEN_TTL);
        properties.setKeyId("test-key-002");

        JwtKeyConfig keyConfig = new JwtKeyConfig();
        RSAKey signingJwk = keyConfig.signingJwk(keyPair, properties);
        JWKSet jwkSet = keyConfig.jwkSet(signingJwk);
        JWKSource<SecurityContext> jwkSource = keyConfig.jwkSource(jwkSet);
        JwtEncoder jwtEncoder = keyConfig.jwtEncoder(jwkSource);

        jwtProvider = new JwtProvider(
                jwtEncoder,
                properties,
                Clock.fixed(issuedAt, ZoneOffset.UTC)
        );
        jwtDecoder = new SecurityConfig().jwtDecoder(properties, jwkSource);
    }

    @Test
    void createAccessTokenUsesSharedJwkSourceAndConfiguredClaims() {
        UUID accountUuid = UUID.randomUUID();

        String token = jwtProvider.createAccessToken(accountUuid, AccountRole.USER);

        Jwt parsed = jwtDecoder.decode(token);
        assertEquals("test-key-002", parsed.getHeaders().get("kid"));
        assertEquals("RS256", parsed.getHeaders().get("alg"));
        assertEquals("JWT", parsed.getHeaders().get("typ"));
        assertEquals("test-issuer", parsed.getClaimAsString("iss"));
        assertEquals(accountUuid.toString(), parsed.getSubject());
        assertEquals(
                Set.of("account-api", "inventory-api"),
                Set.copyOf(parsed.getAudience())
        );
        assertEquals(issuedAt, parsed.getIssuedAt());
        assertEquals(issuedAt.plus(ACCESS_TOKEN_TTL), parsed.getExpiresAt());
    }

    @Test
    void decoderRejectsTokenWithoutAllowedAudience() {
        properties.setIssuedAudiences(
                new LinkedHashSet<>(Set.of("inventory-api"))
        );
        String token = jwtProvider.createAccessToken(UUID.randomUUID(), AccountRole.USER);

        assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(token));
    }
}
