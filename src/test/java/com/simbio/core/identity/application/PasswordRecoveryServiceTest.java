package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;

class PasswordRecoveryServiceTest {

    private static final String VALID_PASSWORD = "synthetic-password-123";
    private final UserAccountStore accounts = mock(UserAccountStore.class);
    private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final SessionInvalidator sessions = mock(SessionInvalidator.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final PasswordRecoveryService service = new PasswordRecoveryService(
            accounts, tokens, new PasswordPolicy(), passwordEncoder, emailSender,
            new IdentityProperties(), sessions, auditRecorder, "https://portal.example.test");

    @Test
    void resetRequestIsGenericForInvalidUnknownAndUnverifiedAddresses() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity unverified = account(userId, "es", false);
        when(accounts.findByEmail("unknown@example.test")).thenReturn(Optional.empty());
        when(accounts.findByEmail("unverified@example.test")).thenReturn(Optional.of(unverified));

        service.requestReset(null);
        service.requestReset("x".repeat(321));
        service.requestReset(" Unknown@Example.Test ");
        service.requestReset("unverified@example.test");

        verify(tokens, never()).issueForUser(any(), any(), any());
        verify(emailSender, never()).enqueue(any());
    }

    @Test
    void resetRequestIssuesLocalizedTokenForVerifiedAccount() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = account(userId, "en", true);
        when(accounts.findByEmail("verified@example.test")).thenReturn(Optional.of(account));
        when(tokens.issueForUser(eq(userId), eq(TokenPurpose.PASSWORD_RESET), any(Duration.class)))
                .thenReturn("synthetic-reset-token");

        service.requestReset(" Verified@Example.Test ");

        verify(tokens).revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emailSender).enqueue(command.capture());
        assertThat(command.getValue().recipientUserId()).isEqualTo(userId);
        assertThat(command.getValue().template()).isEqualTo(EmailTemplate.PASSWORD_RECOVERY);
        assertThat(command.getValue().locale().getLanguage()).isEqualTo("en");
        assertThat(command.getValue().variables().get("url"))
                .isEqualTo("https://portal.example.test/restablecer?token=synthetic-reset-token");
    }

    @Test
    void resetPasswordRejectsInvalidTokensAndUnavailableAccounts() {
        UUID userId = UUID.randomUUID();
        when(tokens.consume("missing-user-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(new ConsumedToken(null, null, null)));
        when(tokens.consume("unknown-account-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(new ConsumedToken(userId, null, null)));
        when(accounts.findByPublicId(userId)).thenReturn(Optional.empty());

        assertThat(service.resetPassword("token", "short")).isFalse();
        assertThat(service.resetPassword("token", VALID_PASSWORD)).isFalse();
        assertThat(service.resetPassword("missing-user-token", VALID_PASSWORD)).isFalse();
        assertThat(service.resetPassword("unknown-account-token", VALID_PASSWORD)).isFalse();
        verify(accounts, never()).save(any());
        verify(sessions, never()).invalidate(any());
    }

    @Test
    void resetPasswordUpdatesVerifiedAccountAndInvalidatesSessions() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = account(userId, "es", true);
        when(tokens.consume("valid-reset-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(new ConsumedToken(userId, null, null)));
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encoded-new-password");

        assertThat(service.resetPassword("valid-reset-token", VALID_PASSWORD)).isTrue();

        assertThat(account.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(accounts).save(account);
        verify(tokens).revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
        verify(sessions).invalidate(userId);
        verify(emailSender).enqueue(any(EmailCommand.class));
        verify(auditRecorder).record(any(AuditRecord.class));
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndAcceptedNewPassword() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = account(userId, "en", true);
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encoded-new-password");

        assertThat(service.changePassword(userId, "current-password", "short")).isFalse();
        assertThat(service.changePassword(userId, null, VALID_PASSWORD)).isFalse();
        assertThat(service.changePassword(userId, "wrong-password", VALID_PASSWORD)).isFalse();
        assertThat(service.changePassword(userId, "current-password", VALID_PASSWORD)).isTrue();

        verify(accounts).save(account);
        verify(sessions).invalidate(userId);
        verify(tokens).revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
        verify(emailSender).enqueue(any(EmailCommand.class));
        verify(auditRecorder).record(any(AuditRecord.class));
    }

    private static UserAccountEntity account(UUID userId, String locale, boolean emailVerified) {
        return new UserAccountEntity(userId, "synthetic@example.test", "old-hash", locale,
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("CLIENT", "Client"),
                "Synthetic user", emailVerified);
    }
}
