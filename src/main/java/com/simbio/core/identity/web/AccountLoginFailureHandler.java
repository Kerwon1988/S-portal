package com.simbio.core.identity.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.UserAccountStore;

@Component
public class AccountLoginFailureHandler implements AuthenticationFailureHandler {

    private final UserAccountStore accounts;
    private final Clock clock;

    public AccountLoginFailureHandler(UserAccountStore accounts) {
        this(accounts, Clock.systemUTC());
    }

    AccountLoginFailureHandler(UserAccountStore accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && email.length() <= 320) {
            accounts.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(account -> {
                account.recordFailedLogin(Instant.now(clock));
                accounts.save(account);
            });
        }
        response.sendRedirect("/acceso?error");
    }
}
