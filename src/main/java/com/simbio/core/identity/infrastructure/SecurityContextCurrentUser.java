package com.simbio.core.identity.infrastructure;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.CurrentAuthenticatedUser;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.CurrentUser;

@Component
class SecurityContextCurrentUser implements CurrentUser, CurrentAuthenticatedUser {

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<AuthenticatedUser> currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof PortalUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal.authenticatedUser());
    }
}
