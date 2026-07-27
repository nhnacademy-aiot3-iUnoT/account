package com.nhnacademy.auth.jwt;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public final class AccountUuidArgumentResolver
        implements HandlerMethodArgumentResolver {

    private final AuthenticationTrustResolver trustResolver =
            new AuthenticationTrustResolverImpl();

    @Override
    public boolean supportsParameter(
            MethodParameter parameter
    ) {
        return parameter.hasParameterAnnotation(
                AccountUUID.class
        ) && parameter.getParameterType()
                .equals(UUID.class);
    }

    @Override
    public UUID resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest request,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || trustResolver.isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "인증 정보가 없습니다."
            );
        }

        String accountUuid = (String) authentication.getPrincipal();

        if (accountUuid == null
                || accountUuid.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "계정 식별자가 없습니다."
            );
        }

        try {
            return UUID.fromString(accountUuid);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationCredentialsNotFoundException(
                    "계정 식별자가 UUID 형식이 아닙니다."
            );
        }
    }
}