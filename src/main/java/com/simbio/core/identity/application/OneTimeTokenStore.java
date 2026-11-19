package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.identity.domain.OneTimeToken;

public interface OneTimeTokenStore {

    void save(OneTimeToken token);

    Optional<OneTimeToken> find(String tokenHash, String purpose);

    Page<OneTimeToken> findPendingInvitations(Set<String> roleCodes, Pageable pageable);

    Optional<OneTimeToken> findPendingInvitationForUpdate(UUID id, String roleCode);

    boolean consume(UUID id, Instant now);

    void revokeForUser(UUID userId, String purpose, Instant now);

    void revokeInvitations(Set<String> emailKeys, Instant now);
}
