package com.simbio.core.identity.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.UserAccountStore;

@Component
public class AccountLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserAccountStore accounts;

    public AccountLoginSuccessHandler(UserAccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        accounts.findByEmail(authentication.getName()).ifPresent(account -> {
            account.resetLoginFailures();
            accounts.save(account);
        });
        String destination = request.getParameter("continue");
        if (destination == null || !destination.startsWith("/") || destination.startsWith("//")
                || destination.startsWith("/\\") || destination.contains("\r") || destination.contains("\n")) {
            destination = "/gestion/auditoria";
        }
        response.sendRedirect(destination);
    }
}
