package com.simbio.core.identity.infrastructure;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.CurrentAuthenticatedUser;
import com.simbio.core.identity.application.UserAccountStore;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.LocalePreference;
import com.simbio.core.shared.spi.RecipientDirectory;
import com.simbio.core.shared.spi.ThemePreference;
import com.simbio.core.shared.value.EmailAddress;

@Component
class SecurityContextCurrentUser implements CurrentUser, CurrentAuthenticatedUser,
        RecipientDirectory, ThemePreference, LocalePreference {

    private final UserAccountStore accounts;

    SecurityContextCurrentUser(UserAccountStore accounts) {
        this.accounts = accounts;
    }

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

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<EmailAddress> emailAddressOf(java.util.UUID userId) {
        return accounts.findByPublicId(userId).map(account -> EmailAddress.of(account.getEmailNormalized()));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<String> findTheme(java.util.UUID userId) {
        return accounts.findByPublicId(userId).map(account -> account.getThemePreference())
                .filter(java.util.Objects::nonNull);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void saveTheme(java.util.UUID userId, String theme) {
        accounts.findByPublicId(userId).ifPresent(account -> {
            account.updateTheme(theme);
            accounts.save(account);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<String> findLocale(java.util.UUID userId) {
        return accounts.findByPublicId(userId).map(account -> account.getPreferredLocale());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void saveLocale(java.util.UUID userId, String locale) {
        accounts.findByPublicId(userId).ifPresent(account -> {
            account.updateLocale(locale);
            accounts.save(account);
        });
    }
}
