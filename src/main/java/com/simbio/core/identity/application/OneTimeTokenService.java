package com.simbio.core.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.domain.OneTimeToken;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class OneTimeTokenService {

    private final OneTimeTokenStore store;
    private final SensitiveDataProtector dataProtector;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public OneTimeTokenService(OneTimeTokenStore store, SensitiveDataProtector dataProtector) {
        this(store, dataProtector, new SecureRandom(), Clock.systemUTC());
    }

    OneTimeTokenService(
            OneTimeTokenStore store,
            SensitiveDataProtector dataProtector,
            SecureRandom secureRandom,
            Clock clock) {
        this.store = store;
        this.dataProtector = dataProtector;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public String issueForUser(UUID userId, TokenPurpose purpose, Duration ttl) {
        if (purpose == TokenPurpose.INVITATION) {
            throw new IllegalArgumentException("Invitation tokens require an invitation target");
        }
        return issue(purpose, userId, null, null, null, null, ttl);
    }

    @Transactional
    public String issueInvitation(String email, String roleCode, Duration ttl) {
        return issueInvitation(email, roleCode, null, ttl);
    }

    @Transactional
    public String issueInvitation(String email, String roleCode, UUID invitationReference, Duration ttl) {
        if (!"CLIENT".equals(roleCode) && !"EMPLOYEE".equals(roleCode)) {
            throw new IllegalArgumentException("Only client and employee invitations are allowed");
        }
        String normalizedEmail = normalizeEmail(email);
        return issue(TokenPurpose.INVITATION, null, dataProtector.protect(normalizedEmail),
                dataProtector.fingerprint("invitation-email", normalizedEmail), roleCode, invitationReference, ttl);
    }

    @Transactional
    public Optional<ConsumedToken> consume(String rawToken, TokenPurpose purpose) {
        if (rawToken == null || rawToken.length() < 32 || rawToken.length() > 128) {
            return Optional.empty();
        }
        String hash = hash(rawToken);
        Optional<OneTimeToken> candidate = store.find(hash, purpose.name());
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        OneTimeToken token = candidate.get();
        byte[] stored = token.getTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = hash.getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(stored, supplied)) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        if (token.getExpiresAt().compareTo(now) <= 0 || token.getConsumedAt() != null
                || token.getRevokedAt() != null || !store.consume(token.getId(), now)) {
            return Optional.empty();
        }
        String email = token.getEmailEncrypted() == null ? null : dataProtector.reveal(token.getEmailEncrypted());
        return Optional.of(new ConsumedToken(token.getUserPublicId(), email, token.getRoleCode(),
                token.getInvitationReference()));
    }

    @Transactional
    public void revokeForUser(UUID userId, TokenPurpose purpose) {
        store.revokeForUser(userId, purpose.name(), Instant.now(clock));
    }

    @Transactional
    public void revokeInvitations(String email) {
        String normalizedEmail = normalizeEmail(email);
        Set<String> emailKeys = new HashSet<>(
                dataProtector.fingerprintCandidates("invitation-email", normalizedEmail));
        emailKeys.add(hash(normalizedEmail));
        store.revokeInvitations(
                Set.copyOf(emailKeys),
                Instant.now(clock));
    }

    private String issue(
            TokenPurpose purpose,
            UUID userId,
            String encryptedEmail,
            String emailKey,
            String roleCode,
            UUID invitationReference,
            Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        Instant now = Instant.now(clock);
        store.save(new OneTimeToken(UUID.randomUUID(), hash(rawToken), purpose.name(), userId,
                encryptedEmail, emailKey, roleCode, invitationReference, now.plus(ttl), now));
        return rawToken;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 320) {
            throw new IllegalArgumentException("Invitation email is invalid");
        }
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }
}
