package com.nhnacademy.account.controller;

import com.nhnacademy.account.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkSetController {
    private final RSAPublicKey publicKey;
    private final JwtProperties properties;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwkSet() {
        Map<String, String> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", properties.getKeyId(),
                "n", base64UrlUnsigned(publicKey.getModulus()),
                "e", base64UrlUnsigned(publicKey.getPublicExponent())
        );
        return Map.of("keys", List.of(key));
    }

    private String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
