package com.nhnacademy.account.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkSetController {
    private final JWKSet jwkSet;

    @GetMapping("/api/auth/.well-known/jwks.json")
    public Map<String, Object> jwkSetTemp() {
        return jwkSet.toJSONObject(true);
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwkSet() {
        return jwkSet.toJSONObject(true);
    }

}
