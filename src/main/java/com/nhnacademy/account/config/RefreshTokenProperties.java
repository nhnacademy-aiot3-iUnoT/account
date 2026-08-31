package com.nhnacademy.account.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("security.refresh-token")
public class RefreshTokenProperties {

    @NotNull
    private Duration ttl;

    @AssertTrue(message = "security.refresh-token.ttl must be positive")
    public boolean isTtlPositive() {
        return ttl == null || ttl.compareTo(Duration.ZERO) > 0;
    }
}
