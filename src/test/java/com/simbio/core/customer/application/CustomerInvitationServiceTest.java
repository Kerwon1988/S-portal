package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.ClientRegistered;
import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.domain.ClientInvitationEntity;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

class CustomerInvitationServiceTest {

    private final ClientInvitationStore store = org.mockito.Mockito.mock(ClientInvitationStore.class);
    private final InvitationIssuer issuer = org.mockito.Mockito.mock(InvitationIssuer.class);
    private final CustomerAccountCreator accounts = org.mockito.Mockito.mock(CustomerAccountCreator.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final SensitiveDataProtector protector = org.mockito.Mockito.mock(SensitiveDataProtector.class);
    private final CustomerInvitationService service = new CustomerInvitationService(
            store, issuer, accounts, auditRecorder, events, protector,
            Clock.fixed(Instant.parse("2026-11-18T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void deniesEmployeeWithoutCreatingInvitation() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.USERS_MANAGE));

        assertThatThrownBy(() -> service.inviteClient(employee, "synthetic@example.test",
                "COMPANY", "Synthetic company", Locale.ENGLISH))
                .isInstanceOf(AccessDeniedException.class);

        verify(issuer, never()).invite(any(), any(), any(), any(), any());
        verify(store, never()).save(any(ClientInvitationEntity.class));
    }

    @Test
    void correlatesInvitationWithoutPersistingEmailAndCreatesAccountOnceAfterAcceptance() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(adminId, Role.ADMIN, Set.of(Permission.USERS_MANAGE));
        when(issuer.invite(eq(admin), eq("synthetic@example.test"), eq("CLIENT"),
                eq(Locale.ENGLISH), any(UUID.class)))
                .thenReturn(true);
        when(protector.protect("Synthetic company")).thenReturn("encrypted-account-name");
        when(protector.reveal("encrypted-account-name")).thenReturn("Synthetic company");
        when(store.save(any(ClientInvitationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.createOwnerAccount(adminId, userId, "COMPANY", "Synthetic company"))
                .thenReturn(new CustomerAccountSummary(accountId, "COMPANY", "Synthetic company",
                        "ACTIVE", Instant.parse("2026-11-18T08:01:00Z")));

        UUID invitationReference = service.inviteClient(admin, "synthetic@example.test",
                "COMPANY", "Synthetic company", Locale.ENGLISH).orElseThrow();
        var saved = org.mockito.ArgumentCaptor.forClass(ClientInvitationEntity.class);
        verify(store).save(saved.capture());
        assertThat(saved.getValue().getInvitationReference()).isEqualTo(invitationReference);
        assertThat(saved.getValue().getAccountNameCiphertext()).isEqualTo("encrypted-account-name");
        assertThat(saved.getValue().toString()).doesNotContain("synthetic@example.test", "Synthetic company");

        when(store.findByReferenceForUpdate(invitationReference)).thenReturn(Optional.of(saved.getValue()));
        TransactionSynchronizationManager.initSynchronization();
        try {
            InvitationAccepted event = new InvitationAccepted(userId, "CLIENT", invitationReference);
            service.completeInvitation(event);
            service.completeInvitation(event);

            verify(accounts, times(1)).createOwnerAccount(
                    adminId, userId, "COMPANY", "Synthetic company");
            verify(store, times(2)).save(saved.getValue());
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCommit());
            verify(events).publishEvent(new ClientRegistered(accountId, userId));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
