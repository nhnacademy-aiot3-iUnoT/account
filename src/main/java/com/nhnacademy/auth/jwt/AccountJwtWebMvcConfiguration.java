package com.nhnacademy.auth.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "nhn.auth.jwt", name = "enabled", havingValue = "true")
public class AccountJwtWebMvcConfiguration implements WebMvcConfigurer {
    private final AccountUuidArgumentResolver resolver;

    public AccountJwtWebMvcConfiguration(AccountUuidArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
