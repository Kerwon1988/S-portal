package com.simbio.core.config.security;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

public class CspNonceFilter extends OncePerRequestFilter {

    private static final String NONCE_ATTRIBUTE = "cspNonce";
    private final SecureRandom secureRandom = new SecureRandom();
    private final Environment environment;

    public CspNonceFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] nonce = new byte[24];
        secureRandom.nextBytes(nonce);
        String encodedNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        request.setAttribute(NONCE_ATTRIBUTE, encodedNonce);
        String headerName = environment.matchesProfiles("prod")
                ? "Content-Security-Policy-Report-Only"
                : "Content-Security-Policy";
        response.setHeader(headerName, "default-src 'self'; script-src 'self' 'nonce-"
                + encodedNonce
                + "'; style-src 'self' 'nonce-"
                + encodedNonce
                + "'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'");
        filterChain.doFilter(request, response);
    }
}
