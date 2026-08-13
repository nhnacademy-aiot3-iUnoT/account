package com.nhnacademy.account.controller;

import com.nhnacademy.account.config.JwtKeyConfig;
import com.nhnacademy.account.config.JwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwkSetControllerTest {

    @Test
    void jwkSetPublishesOnlyConfiguredPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        JwtProperties properties = new JwtProperties();
        properties.setKeyId("test-key-003");

        JwtKeyConfig keyConfig = new JwtKeyConfig();
        RSAKey signingJwk = keyConfig.signingJwk(keyPair, properties);
        JWKSet jwkSet = keyConfig.jwkSet(signingJwk);

        Map<String, Object> response = new JwkSetController(jwkSet).jwkSet();

        List<?> keys = assertInstanceOf(List.class, response.get("keys"));
        assertEquals(1, keys.size());
        Map<?, ?> key = assertInstanceOf(Map.class, keys.getFirst());
        assertEquals("RSA", key.get("kty"));
        assertEquals("sig", key.get("use"));
        assertEquals("RS256", key.get("alg"));
        assertEquals("test-key-003", key.get("kid"));
        assertTrue(key.containsKey("n"));
        assertTrue(key.containsKey("e"));
        assertFalse(key.containsKey("d"));
        assertFalse(key.containsKey("p"));
        assertFalse(key.containsKey("q"));
        assertFalse(key.containsKey("dp"));
        assertFalse(key.containsKey("dq"));
        assertFalse(key.containsKey("qi"));
    }
}
