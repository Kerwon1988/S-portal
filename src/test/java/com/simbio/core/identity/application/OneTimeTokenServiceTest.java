package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simbio.core.identity.domain.OneTimeToken;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.shared.spi.SensitiveDataProtector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OneTimeTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    @Test
    void storesOnlyTokenHashAndConsumesItExactlyOnce() throws Exception {
        OneTimeTokenStore store = mock(OneTimeTokenStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        OneTimeTokenService service = service(store, protector);
        String rawToken = service.issueForUser(
                UUID.randomUUID(), TokenPurpose.PASSWORD_RESET, Duration.ofMinutes(5));

        ArgumentCaptor<OneTimeToken> saved = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(store).save(saved.capture());
        OneTimeToken token = saved.getValue();
        assertThat(rawToken).hasSize(43);
        assertThat(token.getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(token.getTokenHash()).isEqualTo(sha256(rawToken));

        when(store.find(token.getTokenHash(), TokenPurpose.PASSWORD_RESET.name()))
                .thenReturn(Optional.of(token));
        when(store.consume(token.getId(), NOW)).thenReturn(true, false);

        assertThat(service.consume(rawToken, TokenPurpose.PASSWORD_RESET)).isPresent();
        assertThat(service.consume(rawToken, TokenPurpose.PASSWORD_RESET)).isEmpty();
        verify(store, org.mockito.Mockito.times(2)).consume(token.getId(), NOW);
        verify(protector, never()).reveal(anyString());
    }

    @Test
    void rejectsExpiredTokensWithoutAttemptingAtomicConsumption() throws Exception {
        OneTimeTokenStore store = mock(OneTimeTokenStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        OneTimeTokenService service = service(store, protector);
        String rawToken = "synthetic-expired-token-value-123456";
        String tokenHash = sha256(rawToken);
        OneTimeToken expired = new OneTimeToken(
                UUID.randomUUID(),
                tokenHash,
                TokenPurpose.PASSWORD_RESET.name(),
                UUID.randomUUID(),
                null,
                null,
                NOW,
                NOW.minusSeconds(30));
        when(store.find(tokenHash, TokenPurpose.PASSWORD_RESET.name()))
                .thenReturn(Optional.of(expired));

        assertThat(service.consume(rawToken, TokenPurpose.PASSWORD_RESET)).isEmpty();

        verify(store, never()).consume(eq(expired.getId()), eq(NOW));
    }

    @Test
    void protectsInvitationEmailAndAllowsOnlySupportedRoles() throws Exception {
        OneTimeTokenStore store = mock(OneTimeTokenStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.protect("synthetic@example.test")).thenReturn("encrypted-email");
        when(protector.fingerprint("invitation-email", "synthetic@example.test"))
                .thenReturn("keyed-email-index");
        when(protector.fingerprintCandidates("invitation-email", "synthetic@example.test"))
                .thenReturn(Set.of("keyed-email-index", "previous-key-email-index"));
        when(protector.reveal("encrypted-email")).thenReturn("synthetic@example.test");
        OneTimeTokenService service = service(store, protector);

        String rawToken = service.issueInvitation(" Synthetic@Example.Test ", "EMPLOYEE", Duration.ofDays(1));

        ArgumentCaptor<OneTimeToken> saved = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(store).save(saved.capture());
        OneTimeToken invitation = saved.getValue();
        assertThat(invitation.getEmailEncrypted()).isEqualTo("encrypted-email");
        assertThat(invitation.getEmailKey()).isEqualTo("keyed-email-index");
        assertThat(invitation.getRoleCode()).isEqualTo("EMPLOYEE");
        when(store.find(invitation.getTokenHash(), TokenPurpose.INVITATION.name()))
                .thenReturn(Optional.of(invitation));
        when(store.consume(invitation.getId(), NOW)).thenReturn(true);

        assertThat(service.consume(rawToken, TokenPurpose.INVITATION))
                .get()
                .extracting(ConsumedToken::invitationEmail)
                .isEqualTo("synthetic@example.test");
        service.revokeInvitations(" Synthetic@Example.Test ");
        verify(store).revokeInvitations(
                Set.of("keyed-email-index", "previous-key-email-index",
                        sha256("synthetic@example.test")), NOW);

        assertThatThrownBy(() -> service.issueInvitation(
                "synthetic@example.test", "ADMIN", Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only client and employee invitations are allowed");
    }

    @Test
    void rejectsInvalidTokenLifetimeBeforePersistence() {
        OneTimeTokenStore store = mock(OneTimeTokenStore.class);
        OneTimeTokenService service = service(store, mock(SensitiveDataProtector.class));

        assertThatThrownBy(() -> service.issueForUser(
                UUID.randomUUID(), TokenPurpose.PASSWORD_RESET, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token lifetime must be positive");
        verify(store, never()).save(org.mockito.ArgumentMatchers.any(OneTimeToken.class));
    }

    private static OneTimeTokenService service(OneTimeTokenStore store, SensitiveDataProtector protector) {
        return new OneTimeTokenService(
                store, protector, new java.security.SecureRandom(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
