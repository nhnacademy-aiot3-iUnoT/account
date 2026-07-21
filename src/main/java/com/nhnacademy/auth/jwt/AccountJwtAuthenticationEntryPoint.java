package com.nhnacademy.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class AccountJwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final byte[] BODY = ("""
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":"A valid bearer token is required"}
            """).strip().getBytes(StandardCharsets.UTF_8);

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(BODY);
    }
}
