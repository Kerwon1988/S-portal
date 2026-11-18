package com.simbio.core.identity.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.LoginAttemptService;
import com.simbio.core.identity.application.MfaService;
import com.simbio.core.identity.security.PortalUserPrincipal;

@Component
public class AccountLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttempts;
    private final MfaService mfaService;

    public AccountLoginSuccessHandler(LoginAttemptService loginAttempts, MfaService mfaService) {
        this.loginAttempts = loginAttempts;
        this.mfaService = mfaService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        loginAttempts.resetFailures(authentication.getName());
        String destination = request.getParameter("continue");
        if (destination == null) {
            destination = "/es";
        } else if (!destination.startsWith("/") || destination.startsWith("//")
                || destination.startsWith("/\\") || destination.contains("\r") || destination.contains("\n")) {
            destination = "/es";
        }
        if (authentication.getPrincipal() instanceof PortalUserPrincipal principal) {
            boolean enrolled = mfaService.isEnrolled(principal.publicUserId());
            if (principal.isAdmin() || enrolled) {
                var session = request.getSession(true);
                session.setAttribute("mfaContinue", destination);
                session.setAttribute("mfaPending", true);
                response.sendRedirect(enrolled
                        ? "/cuenta/seguridad/mfa"
                        : "/cuenta/seguridad/mfa/enrolar");
                return;
            }
        }
        response.sendRedirect(destination);
    }
}
