package com.simbio.core.identity.infrastructure;

import java.time.Instant;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.identity.domain.OneTimeToken;

interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHashAndPurpose(String tokenHash, String purpose);

    @Modifying
    @Query("""
            update OneTimeToken token set token.consumedAt = :now
            where token.id = :id and token.consumedAt is null and token.revokedAt is null
              and token.expiresAt > :now
            """)
    int consumeIfActive(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("""
            update OneTimeToken token set token.revokedAt = :now
            where token.userPublicId = :userId and token.purpose = :purpose
              and token.consumedAt is null and token.revokedAt is null
            """)
    int revokeActiveForUser(
            @Param("userId") UUID userId,
            @Param("purpose") String purpose,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update OneTimeToken token set token.revokedAt = :now
            where token.emailKey in :emailKeys and token.purpose = 'INVITATION'
              and token.consumedAt is null and token.revokedAt is null
            """)
    int revokeInvitations(@Param("emailKeys") Set<String> emailKeys, @Param("now") Instant now);
}
