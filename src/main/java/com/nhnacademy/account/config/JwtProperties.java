package com.nhnacademy.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@ConfigurationProperties("security.jwt")
public class JwtProperties {
    @Setter
    private File privateKeyPath;

    @Setter
    private URI jwkSetUri;

    @Setter
    private String issuer;

    private Set<String> audiences = new LinkedHashSet<>();

    @Setter
    private Set<SignatureAlgorithm> allowedAlgorithms =
            new LinkedHashSet<>(Set.of(SignatureAlgorithm.RS256));

    public void setAudiences(Set<String> audiences) {
        this.audiences = audiences == null ? new LinkedHashSet<>() : audiences;
    }
}
