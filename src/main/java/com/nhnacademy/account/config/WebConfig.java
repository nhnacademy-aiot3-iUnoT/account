package com.nhnacademy.account.config;

import com.nhnacademy.account.security.AccountUuidArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AccountUuidArgumentResolver accountUuidArgumentResolver;

    public WebConfig(AccountUuidArgumentResolver accountUuidArgumentResolver) {
        this.accountUuidArgumentResolver = accountUuidArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(accountUuidArgumentResolver);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
