package com.nhnacademy.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Getter
@Validated
@ConfigurationProperties("security.jwt")
public class JwtProperties {
    @Setter
    private File privateKeyPath;

    @Setter
    @NotBlank
    private String issuer;

    private Set<String> audiences = new LinkedHashSet<>();

    @NotEmpty
    private Set<@NotBlank String> issuedAudiences = new LinkedHashSet<>();

    @Setter
    @NotNull
    private Duration accessTokenTtl;

    @Setter
    @NotBlank
    private String keyId;

    @Setter
    private Set<SignatureAlgorithm> allowedAlgorithms =
            new LinkedHashSet<>(Set.of(SignatureAlgorithm.RS256));

    public void setAudiences(Set<String> audiences) {
        this.audiences = copyOf(audiences);
    }

    public Set<String> getIssuedAudiences() {
        return Set.copyOf(issuedAudiences);
    }

    public void setIssuedAudiences(Set<String> issuedAudiences) {
        this.issuedAudiences = copyOf(issuedAudiences);
    }

    @AssertTrue(message = "security.jwt.access-token-ttl must be positive")
    public boolean isAccessTokenTtlPositive() {
        return accessTokenTtl == null || accessTokenTtl.compareTo(Duration.ZERO) > 0;
    }

    private LinkedHashSet<String> copyOf(Set<String> values) {
        return values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }
}
