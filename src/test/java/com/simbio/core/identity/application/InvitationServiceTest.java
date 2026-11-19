package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.OneTimeToken;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.spi.SensitiveDataProtector;

class InvitationServiceTest {

    private static final String VALID_PASSWORD = "synthetic-password-123";
    private final UserAccountStore accounts = mock(UserAccountStore.class);
    private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
    private final OneTimeTokenStore invitationTokens = mock(OneTimeTokenStore.class);
    private final SensitiveDataProtector dataProtector = mock(SensitiveDataProtector.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ConsentRecorder consents = mock(ConsentRecorder.class);
    private final InvitationService service = new InvitationService(
            accounts, tokens, invitationTokens, dataProtector, new PasswordPolicy(), passwordEncoder, emailSender,
            new IdentityProperties(), auditRecorder, events, consents, "https://portal.example.test");

    @Test
    void inviteRequiresRoleSpecificAdministratorPermission() {
        UUID actorId = UUID.randomUUID();

        assertThatThrownBy(() -> service.invite(
                new AuthenticatedUser(actorId, Role.CLIENT, Set.of()), "synthetic@example.test", "CLIENT",
                Locale.ENGLISH))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.invite(
                admin(actorId, Permission.USERS_MANAGE), "synthetic@example.test", "EMPLOYEE", Locale.ENGLISH))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.invite(
                admin(actorId, Permission.USERS_MANAGE), "synthetic@example.test", "ADMIN", Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only client and employee invitations are allowed");

        verify(accounts, never()).findByEmail(any());
    }

    @Test
    void inviteReturnsFalseForExistingAccountAndSendsOnlyAuthorizedInvitation() {
        UUID actorId = UUID.randomUUID();
        UUID invitationReference = UUID.randomUUID();
        AuthenticatedUser actor = admin(actorId, Permission.STAFF_MANAGE);
        when(accounts.findByEmail("existing@example.test"))
                .thenReturn(Optional.of(account(UUID.randomUUID(), "EMPLOYEE")));
        when(accounts.findByEmail("new@example.test")).thenReturn(Optional.empty());
        when(tokens.issueInvitation("new@example.test", "EMPLOYEE", invitationReference,
                new IdentityProperties().getInvitationTtl())).thenReturn("synthetic-invitation-token");

        assertThat(service.invite(actor, "existing@example.test", "EMPLOYEE", Locale.ENGLISH)).isFalse();
        assertThat(service.invite(actor, "New@Example.Test", "EMPLOYEE", Locale.ENGLISH, invitationReference))
                .isTrue();

        verify(tokens).revokeInvitations("new@example.test");
        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emailSender).enqueue(command.capture());
        assertThat(command.getValue().template()).isEqualTo(EmailTemplate.INVITATION);
        assertThat(command.getValue().locale().getLanguage()).isEqualTo("en");
        assertThat(command.getValue().variables().get("url"))
                .isEqualTo("https://portal.example.test/invitacion/aceptar?token=synthetic-invitation-token");
        verify(auditRecorder).record(any(AuditRecord.class));
    }

    @Test
    void pendingInvitationListIsPermissionScopedAndMasksRecipient() {
        UUID invitationId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-18T10:00:00Z");
        Instant expiresAt = Instant.now().plusSeconds(3600);
        OneTimeToken invitation = pendingInvitation(invitationId, "CLIENT", UUID.randomUUID(),
                createdAt, expiresAt);
        when(invitationTokens.findPendingInvitations(Set.of("CLIENT"), PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(invitation), PageRequest.of(0, 25), 1));
        when(dataProtector.reveal("encrypted-recipient")).thenReturn("synthetic@example.test");

        var result = service.pendingInvitations(
                admin(UUID.randomUUID(), Permission.USERS_MANAGE), "CLIENT", new PageQuery(0, 25));

        assertThat(result.content()).containsExactly(new PendingInvitationSummary(
                invitationId, "s***@example.test", "CLIENT", createdAt, expiresAt, false));
        assertThat(result.toString()).doesNotContain("synthetic@example.test");
        assertThatThrownBy(() -> service.pendingInvitations(
                admin(UUID.randomUUID(), Permission.USERS_MANAGE), "EMPLOYEE", new PageQuery(0, 25)))
                .isInstanceOf(AccessDeniedException.class);
        verify(invitationTokens, never()).findPendingInvitations(
                Set.of("EMPLOYEE"), PageRequest.of(0, 25));
    }

    @Test
    void resendRotatesPendingTokenAndPreservesInvitationReference() {
        UUID actorId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        UUID invitationReference = UUID.randomUUID();
        OneTimeToken invitation = pendingInvitation(
                invitationId, "CLIENT", invitationReference,
                Instant.parse("2026-11-18T10:00:00Z"), Instant.now().plusSeconds(3600));
        when(invitationTokens.findPendingInvitationForUpdate(invitationId, "CLIENT"))
                .thenReturn(Optional.of(invitation));
        when(dataProtector.reveal("encrypted-recipient")).thenReturn("synthetic@example.test");
        when(tokens.issueInvitation("synthetic@example.test", "CLIENT", invitationReference,
                new IdentityProperties().getInvitationTtl())).thenReturn("fresh-invitation-token");

        assertThat(service.resendInvitation(
                admin(actorId, Permission.USERS_MANAGE), invitationId, "CLIENT", Locale.ENGLISH)).isTrue();

        assertThat(invitation.getRevokedAt()).isNotNull();
        verify(invitationTokens).save(invitation);
        verify(tokens).issueInvitation("synthetic@example.test", "CLIENT", invitationReference,
                new IdentityProperties().getInvitationTtl());
        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emailSender).enqueue(command.capture());
        assertThat(command.getValue().template()).isEqualTo(EmailTemplate.INVITATION);
        assertThat(command.getValue().locale().getLanguage()).isEqualTo("en");
        assertThat(command.getValue().variables().get("url"))
                .isEqualTo("https://portal.example.test/invitacion/aceptar?token=fresh-invitation-token");
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(record ->
                record.actorId().equals(actorId) && "INVITATION_RESENT".equals(record.actionCode())
                        && invitationId.equals(record.entityPublicId())));
    }

    @Test
    void resendDoesNotRevealOrChangeInvitationsOutsideTheActorsScope() {
        UUID invitationId = UUID.randomUUID();
        AuthenticatedUser clientManager = admin(UUID.randomUUID(), Permission.USERS_MANAGE);

        assertThatThrownBy(() -> service.resendInvitation(
                clientManager, invitationId, "EMPLOYEE", Locale.ENGLISH))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.resendInvitation(
                clientManager, invitationId, "CLIENT", Locale.ENGLISH)).isFalse();
        verify(invitationTokens, never()).save(org.mockito.ArgumentMatchers.any(OneTimeToken.class));
        verify(tokens, never()).issueInvitation(
                any(), any(), any(UUID.class), any());
    }

    @Test
    void acceptRejectsMissingConsentBadPasswordAndInvalidProfile() {
        assertThat(service.accept("token", VALID_PASSWORD, "Synthetic user", "es", false, true)).isFalse();
        assertThat(service.accept("token", "short", "Synthetic user", "es", true, true)).isFalse();
        assertThatThrownBy(() -> service.accept("token", VALID_PASSWORD, " ", "es", true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid invitation profile");
        assertThatThrownBy(() -> service.accept("token", VALID_PASSWORD, "Synthetic user", "fr", true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid invitation profile");
        verify(tokens, never()).consume(any(), eq(TokenPurpose.INVITATION));
    }

    @Test
    void acceptRejectsInvalidOrAlreadyUsedInvitation() {
        UUID userId = UUID.randomUUID();
        when(tokens.consume("missing-token", TokenPurpose.INVITATION)).thenReturn(Optional.empty());
        when(tokens.consume("missing-email-token", TokenPurpose.INVITATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, null, "CLIENT")));
        when(tokens.consume("duplicate-token", TokenPurpose.INVITATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, "existing@example.test", "CLIENT")));
        when(accounts.findByEmail("existing@example.test"))
                .thenReturn(Optional.of(account(UUID.randomUUID(), "CLIENT")));
        when(tokens.consume("invalid-role-token", TokenPurpose.INVITATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, "new@example.test", "ADMIN")));
        when(accounts.findByEmail("new@example.test")).thenReturn(Optional.empty());

        assertThat(service.accept("missing-token", VALID_PASSWORD, "Synthetic user", "es", true, true)).isFalse();
        assertThat(service.accept("missing-email-token", VALID_PASSWORD, "Synthetic user", "es", true, true))
                .isFalse();
        assertThat(service.accept("duplicate-token", VALID_PASSWORD, "Synthetic user", "es", true, true)).isFalse();
        assertThat(service.accept("invalid-role-token", VALID_PASSWORD, "Synthetic user", "es", true, true))
                .isFalse();
        verify(accounts, never()).createUser(any(), any(), any(), any(), any());
    }

    @Test
    void acceptedInvitationCreatesAccountRecordsConsentAndPublishesAfterCommit() {
        UUID userId = UUID.randomUUID();
        UUID invitationReference = UUID.randomUUID();
        UserAccountEntity account = account(userId, "CLIENT");
        when(tokens.consume("valid-token", TokenPurpose.INVITATION))
                .thenReturn(Optional.of(new ConsumedToken(userId, "synthetic@example.test",
                        "CLIENT", invitationReference)));
        when(accounts.findByEmail("synthetic@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encoded-password");
        when(accounts.createUser("synthetic@example.test", "encoded-password", "en", "CLIENT", "Synthetic user"))
                .thenReturn(account);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(service.accept("valid-token", VALID_PASSWORD, " Synthetic user ", "en", true, true))
                    .isTrue();
            verify(consents).recordRequiredAcceptances(
                    new AuthenticatedUser(userId, Role.CLIENT, Set.of()), "en",
                    com.simbio.core.legal.api.ConsentSource.ADMIN_INVITATION);
            verify(auditRecorder).record(any(AuditRecord.class));
            verify(events, never()).publishEvent(any(Object.class));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(events).publishEvent(new InvitationAccepted(userId, "CLIENT", invitationReference));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static AuthenticatedUser admin(UUID userId, Permission... permissions) {
        return new AuthenticatedUser(userId, Role.ADMIN, Set.of(permissions));
    }

    private static UserAccountEntity account(UUID userId, String roleCode) {
        return new UserAccountEntity(userId, "synthetic@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity(roleCode, roleCode));
    }

    private static OneTimeToken pendingInvitation(
            UUID id,
            String roleCode,
            UUID invitationReference,
            Instant createdAt,
            Instant expiresAt) {
        return new OneTimeToken(id, "synthetic-hash", "INVITATION", null, "encrypted-recipient",
                "synthetic-email-key", roleCode, invitationReference, expiresAt, createdAt);
    }
}
