package com.nhnacademy.auth.jwt;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountJwtAutoConfigurationTest {
    private KeyPair keyPair;
    private HttpServer jwkServer;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        jwkServer = HttpServer.create(new InetSocketAddress(0), 0);
        jwkServer.createContext("/.well-known/jwks.json", exchange -> {
            byte[] body = jwkSetJson((RSAPublicKey) keyPair.getPublic()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        jwkServer.start();

        AccountJwtProperties properties = new AccountJwtProperties();
        properties.setJwkSetUri(URI.create("http://localhost:" + jwkServer.getAddress().getPort()
                + "/.well-known/jwks.json"));
        properties.setIssuer("account-api");
        properties.setAudiences(Set.of("account-api"));
        decoder = new AccountJwtAutoConfiguration().accountJwtDecoder(properties);
    }

    @AfterEach
    void tearDown() {
        if (jwkServer != null) {
            jwkServer.stop(0);
        }
    }

    @Test
    void verifiesSignatureClaimsAndUuidSubject() {
        UUID accountUuid = UUID.randomUUID();

        Jwt jwt = decoder.decode(token(accountUuid.toString(), "account-api", "account-api", true));

        assertEquals(accountUuid.toString(), jwt.getSubject());
        assertEquals("account-key-001", jwt.getHeaders().get("kid"));
    }

    @Test
    void rejectsUnexpectedAudience() {
        String encoded = token(UUID.randomUUID().toString(), "account-api", "another-api", true);

        assertThrows(JwtValidationException.class, () -> decoder.decode(encoded));
    }

    @Test
    void rejectsSubjectThatIsNotUuid() {
        String encoded = token("account-1", "account-api", "account-api", true);

        assertThrows(JwtValidationException.class, () -> decoder.decode(encoded));
    }

    @Test
    void rejectsTokenWithoutKid() {
        String encoded = token(UUID.randomUUID().toString(), "account-api", "account-api", false);

        assertThrows(JwtValidationException.class, () -> decoder.decode(encoded));
    }

    @Test
    void usesMemoryCacheAfterJwkSetWasLoaded() {
        decoder.decode(token(UUID.randomUUID().toString(), "account-api", "account-api", true));
        jwkServer.stop(0);
        jwkServer = null;

        Jwt jwt = decoder.decode(token(UUID.randomUUID().toString(), "account-api", "account-api", true));

        assertEquals("account-api", jwt.getClaimAsString("iss"));
    }

    private String token(String subject, String issuer, String audience, boolean includeKid) {
        Instant now = Instant.now();
        var builder = Jwts.builder();
        if (includeKid) {
            builder.header().keyId("account-key-001").and();
        }
        return builder
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String jwkSetJson(RSAPublicKey publicKey) {
        return """
                {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"account-key-001","n":"%s","e":"%s"}]}
                """.formatted(unsigned(publicKey.getModulus().toByteArray()),
                unsigned(publicKey.getPublicExponent().toByteArray()));
    }

    private String unsigned(byte[] bytes) {
        int start = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        byte[] value = java.util.Arrays.copyOfRange(bytes, start, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
