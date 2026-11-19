package com.simbio.core.identity.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.OneTimeTokenStore;
import com.simbio.core.identity.domain.OneTimeToken;

@Repository
class JpaOneTimeTokenStore implements OneTimeTokenStore {

    private final OneTimeTokenJpaRepository tokens;

    JpaOneTimeTokenStore(OneTimeTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public void save(OneTimeToken token) {
        tokens.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OneTimeToken> find(String tokenHash, String purpose) {
        return tokens.findByTokenHashAndPurpose(tokenHash, purpose);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OneTimeToken> findPendingInvitations(Set<String> roleCodes, Pageable pageable) {
        return tokens.findPendingInvitations(roleCodes, pageable);
    }

    @Override
    @Transactional
    public Optional<OneTimeToken> findPendingInvitationForUpdate(UUID id, String roleCode) {
        return tokens.findPendingInvitationForUpdate(id, roleCode);
    }

    @Override
    @Transactional
    public boolean consume(UUID id, Instant now) {
        return tokens.consumeIfActive(id, now) == 1;
    }

    @Override
    @Transactional
    public void revokeForUser(UUID userId, String purpose, Instant now) {
        tokens.revokeActiveForUser(userId, purpose, now);
    }

    @Override
    @Transactional
    public void revokeInvitations(Set<String> emailKeys, Instant now) {
        tokens.revokeInvitations(emailKeys, now);
    }
}
