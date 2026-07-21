package com.nhnacademy.auth.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountUuidArgumentResolverTest {
    private final AccountUuidArgumentResolver resolver = new AccountUuidArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesUuidOnlyFromVerifiedJwtAuthentication() {
        UUID uuid = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(uuid.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID resolved = resolver.resolveArgument(null, null, null, null);

        assertEquals(uuid, resolved);
    }

    @Test
    void rejectsAuthenticationThatDidNotComeFromJwtVerifier() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", "password", java.util.List.of()));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolveArgument(null, null, null, null));
    }
}
