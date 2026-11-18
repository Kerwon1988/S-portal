package com.simbio.core.identity.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.IdentityUserLookup;

@Service
public class IdentityUserLookupService implements IdentityUserLookup {

    private final UserAccountStore userAccountStore;

    public IdentityUserLookupService(UserAccountStore userAccountStore) {
        this.userAccountStore = Objects.requireNonNull(userAccountStore);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return userId != null && userAccountStore.findByPublicId(userId).isPresent();
    }
}
