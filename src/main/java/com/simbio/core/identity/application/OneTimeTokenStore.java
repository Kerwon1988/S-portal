package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.identity.domain.OneTimeToken;

public interface OneTimeTokenStore {

    void save(OneTimeToken token);

    Optional<OneTimeToken> find(String tokenHash, String purpose);

    boolean consume(UUID id, Instant now);

    void revokeForUser(UUID userId, String purpose, Instant now);

    void revokeInvitations(Set<String> emailKeys, Instant now);
}
