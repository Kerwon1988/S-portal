package com.simbio.core.identity.infrastructure;

import java.util.UUID;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.SessionInvalidator;
import com.simbio.core.identity.security.PortalUserPrincipal;

@Component
class RegistrySessionInvalidator implements SessionInvalidator {

    private final SessionRegistry sessionRegistry;

    RegistrySessionInvalidator(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void invalidate(UUID userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(PortalUserPrincipal.class::isInstance)
                .map(PortalUserPrincipal.class::cast)
                .filter(principal -> principal.publicUserId().equals(userId))
                .forEach(principal -> sessionRegistry.getAllSessions(principal, false)
                        .forEach(session -> session.expireNow()));
    }
}
