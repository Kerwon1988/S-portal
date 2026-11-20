package com.simbio.core.identity.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.IdentityOwnerProfile;
import com.simbio.core.identity.api.IdentityOwnerProfileApi;
import com.simbio.core.identity.api.IdentityUserLookup;

@Service
public class IdentityUserLookupService implements IdentityUserLookup, IdentityOwnerProfileApi {

    private final UserAccountStore userAccountStore;

    public IdentityUserLookupService(UserAccountStore userAccountStore) {
        this.userAccountStore = Objects.requireNonNull(userAccountStore);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return userId != null && userAccountStore.findByPublicId(userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityOwnerProfile> findOwnerProfile(UUID userPublicId) {
        if (userPublicId == null) return Optional.empty();
        return userAccountStore.findByPublicId(userPublicId)
                .map(account -> new IdentityOwnerProfile(account.getPublicId(),
                        account.getEmailNormalized(), account.getDisplayName(),
                        account.getPreferredLocale(), account.getEmailVerifiedAt()));
    }
}
