package com.nhnacademy.auth.jwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "nhn.auth.jwt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AccountJwtProperties.class)
public class AccountJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder accountJwtDecoder(AccountJwtProperties properties) {
        Assert.notNull(properties.getJwkSetUri(), "nhn.auth.jwt.jwk-set-uri must be configured");
        Assert.hasText(properties.getIssuer(), "nhn.auth.jwt.issuer must be configured");
        Assert.notEmpty(properties.getAllowedAlgorithms(), "nhn.auth.jwt.allowed-algorithms must not be empty");

        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder =
                NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri().toString());
        properties.getAllowedAlgorithms().forEach(builder::jwsAlgorithm);

        NimbusJwtDecoder decoder = builder.build();
        decoder.setJwtValidator(jwtValidator(properties));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter accountJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    AccountUuidArgumentResolver accountUuidArgumentResolver() {
        return new AccountUuidArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    AccountJwtAuthenticationEntryPoint accountJwtAuthenticationEntryPoint() {
        return new AccountJwtAuthenticationEntryPoint();
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(AccountJwtProperties properties) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        validators.add(jwt -> jwt.getHeaders().get("kid") instanceof String kid && !kid.isBlank()
                ? OAuth2TokenValidatorResult.success()
                : failure("invalid_token", "JWT kid header is required"));
        validators.add(this::validateUuidSubject);
        if (!properties.getAudiences().isEmpty()) {
            validators.add(jwt -> jwt.getAudience().stream().anyMatch(properties.getAudiences()::contains)
                    ? OAuth2TokenValidatorResult.success()
                    : failure("invalid_token", "JWT audience is not allowed"));
        }
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    private OAuth2TokenValidatorResult validateUuidSubject(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException exception) {
            return failure("invalid_token", "JWT subject must be an account UUID");
        }
    }

    private OAuth2TokenValidatorResult failure(String code, String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
    }
}
