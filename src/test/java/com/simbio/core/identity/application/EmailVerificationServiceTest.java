package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.EmailVerified;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;

class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    private final UserAccountStore accounts = mock(UserAccountStore.class);
    private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final EmailVerificationService service = new EmailVerificationService(
            accounts, tokens, emailSender, new IdentityProperties(), events, auditRecorder,
            "https://portal.example.test", Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void sendsVerificationOnlyForAnUnverifiedAccount() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity unverified = account(userId, "en", false);
        UserAccountEntity verified = account(UUID.randomUUID(), "es", true);
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(unverified));
        when(accounts.findByPublicId(verified.getPublicId())).thenReturn(Optional.of(verified));
        when(tokens.issueForUser(eq(userId), eq(TokenPurpose.EMAIL_VERIFICATION), any()))
                .thenReturn("synthetic-verification-token");

        service.sendVerification(userId);
        service.sendVerification(verified.getPublicId());

        verify(tokens).revokeForUser(userId, TokenPurpose.EMAIL_VERIFICATION);
        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emailSender).enqueue(command.capture());
        assertThat(command.getValue().template()).isEqualTo(EmailTemplate.EMAIL_VERIFICATION);
        assertThat(command.getValue().locale().getLanguage()).isEqualTo("en");
        assertThat(command.getValue().variables().get("url"))
                .isEqualTo("https://portal.example.test/verificar-email?token=synthetic-verification-token");
        verify(tokens, never()).issueForUser(
                eq(verified.getPublicId()), eq(TokenPurpose.EMAIL_VERIFICATION), any());
    }

    @Test
    void resendSilentlyIgnoresInvalidAndVerifiedEmailAddresses() {
        UUID verifiedId = UUID.randomUUID();
        when(accounts.findByEmail("verified@example.test"))
                .thenReturn(Optional.of(account(verifiedId, "es", true)));

        service.resend(null);
        service.resend("x".repeat(321));
        service.resend(" Verified@Example.Test ");

        verify(emailSender, never()).enqueue(any());
    }

    @Test
    void verifiesAccountAndPublishesEventAfterCommit() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = account(userId, "es", false);
        when(tokens.consume("synthetic-verification-token", TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, null, null)));
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(account));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(service.verify("synthetic-verification-token")).isTrue();
            assertThat(account.getEmailVerifiedAt()).isEqualTo(NOW);
            verify(accounts).save(account);
            verify(auditRecorder).record(any(AuditRecord.class));
            verify(events, never()).publishEvent(any(Object.class));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(events).publishEvent(new EmailVerified(userId));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void verifyRejectsMissingTokenAndMissingAccount() {
        UUID userId = UUID.randomUUID();
        when(tokens.consume("missing-account-token", TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, null, null)));

        assertThat(service.verify("invalid-token")).isFalse();
        assertThat(service.verify("missing-account-token")).isFalse();
        assertThatThrownBy(() -> service.sendVerification(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account is unavailable");
        verify(accounts, never()).save(any());
    }

    private static UserAccountEntity account(UUID userId, String locale, boolean emailVerified) {
        return new UserAccountEntity(userId, "synthetic@example.test", "hash", locale, NOW,
                new RoleEntity("CLIENT", "Client"), "Synthetic user", emailVerified);
    }
}
