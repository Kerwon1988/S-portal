package com.simbio.core.identity.web;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.LoginAttemptService;

@Component
public class AccountLoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttempts;

    public AccountLoginFailureHandler(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && email.length() <= 320) {
            loginAttempts.recordFailure(email.trim().toLowerCase(Locale.ROOT));
        }
        response.sendRedirect("/acceso?error");
    }
}
