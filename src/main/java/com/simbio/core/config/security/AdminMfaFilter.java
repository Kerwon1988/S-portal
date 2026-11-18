package com.simbio.core.config.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminMfaFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !(path.startsWith("/gestion/") || path.startsWith("/cuenta/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getServletPath();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean mfaComplete = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "MFA_VERIFIED".equals(authority.getAuthority()));
        var session = request.getSession(false);
        boolean employeeMfaPending = session != null && Boolean.TRUE.equals(session.getAttribute("mfaPending"));
        boolean mfaRoute = path.equals("/cuenta/seguridad/mfa")
                || path.startsWith("/cuenta/seguridad/mfa/");
        if ((admin || employeeMfaPending) && !mfaComplete && !mfaRoute) {
            response.sendRedirect("/cuenta/seguridad/mfa");
            return;
        }
        chain.doFilter(request, response);
    }
}
