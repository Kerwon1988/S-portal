package com.simbio.core.identity.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.domain.UserAccountEntity;

@Service
public class ProfileService {

    private final UserAccountStore accounts;

    public ProfileService(UserAccountStore accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public ProfileData get(UUID userId) {
        UserAccountEntity account = requireAccount(userId);
        return new ProfileData(account.getPublicId(), account.getEmailNormalized(),
                account.getDisplayName(), account.getPreferredLocale(), account.getThemePreference());
    }

    @Transactional
    public void update(UUID userId, String displayName, String locale, String theme) {
        UserAccountEntity account = requireAccount(userId);
        account.updateProfile(displayName, locale, theme);
        accounts.save(account);
    }

    private UserAccountEntity requireAccount(UUID userId) {
        return accounts.findByPublicId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account is unavailable"));
    }
}
