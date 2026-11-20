package com.simbio.core.config.security;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class CspNonceFilter extends OncePerRequestFilter {

    private static final String NONCE_ATTRIBUTE = "cspNonce";
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] nonce = new byte[24];
        secureRandom.nextBytes(nonce);
        String encodedNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        request.setAttribute(NONCE_ATTRIBUTE, encodedNonce);
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'nonce-"
                + encodedNonce
                + "'; style-src 'self' 'nonce-"
                + encodedNonce
                + "'; img-src 'self' data:; media-src 'self'; connect-src 'self'; font-src 'self'; "
                + "form-action 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; "
                + "upgrade-insecure-requests");
        filterChain.doFilter(request, response);
    }
}
