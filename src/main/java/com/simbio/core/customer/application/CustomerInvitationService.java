package com.simbio.core.customer.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.ClientRegistered;
import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.customer.domain.ClientInvitationEntity;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class CustomerInvitationService {

    private final ClientInvitationStore invitations;
    private final InvitationIssuer invitationIssuer;
    private final CustomerAccountCreator accountCreator;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;
    private final SensitiveDataProtector dataProtector;
    private final Clock clock;

    @Autowired
    public CustomerInvitationService(
            ClientInvitationStore invitations,
            InvitationIssuer invitationIssuer,
            CustomerAccountCreator accountCreator,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events,
            SensitiveDataProtector dataProtector) {
        this(invitations, invitationIssuer, accountCreator, auditRecorder, events, dataProtector, Clock.systemUTC());
    }

    CustomerInvitationService(
            ClientInvitationStore invitations,
            InvitationIssuer invitationIssuer,
            CustomerAccountCreator accountCreator,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events,
            SensitiveDataProtector dataProtector,
            Clock clock) {
        this.invitations = Objects.requireNonNull(invitations);
        this.invitationIssuer = Objects.requireNonNull(invitationIssuer);
        this.accountCreator = Objects.requireNonNull(accountCreator);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.events = Objects.requireNonNull(events);
        this.dataProtector = Objects.requireNonNull(dataProtector);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public Optional<UUID> inviteClient(
            AuthenticatedUser actor,
            String email,
            String accountType,
            String accountName,
            Locale locale) {
        requirePermission(actor);
        ClientAccountType type;
        try {
            type = ClientAccountType.valueOf(accountType);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unsupported account type", exception);
        }
        if (accountName == null || accountName.isBlank() || accountName.trim().length() > 160) {
            throw new IllegalArgumentException("Account name must contain 1 to 160 characters");
        }
        UUID invitationReference = UUID.randomUUID();
        if (!invitationIssuer.invite(actor, email, "CLIENT", locale, invitationReference)) {
            return Optional.empty();
        }
        invitations.save(new ClientInvitationEntity(invitationReference, type.name(),
                dataProtector.protect(accountName.trim()), actor.userId(), Instant.now(clock)));
        auditRecorder.record(new AuditRecord(actor.userId(), "CLIENT_INVITATION_CREATED",
                "CLIENT_INVITATION", invitationReference, null));
        return Optional.of(invitationReference);
    }

    @Transactional
    public void completeInvitation(InvitationAccepted event) {
        if (event == null || !"CLIENT".equals(event.roleCode()) || event.invitationReference() == null) {
            return;
        }
        ClientInvitationEntity invitation = invitations.findByReferenceForUpdate(event.invitationReference())
                .orElse(null);
        if (invitation == null || !"PENDING".equals(invitation.getStatus())) {
            return;
        }
        CustomerAccountSummary account = accountCreator.createOwnerAccount(
                invitation.getCreatedByUserId(), event.userId(),
                invitation.getAccountType(), dataProtector.reveal(invitation.getAccountNameCiphertext()));
        invitation.complete(event.userId(), account.publicId(), Instant.now(clock));
        invitations.save(invitation);
        publishAfterCommit(new ClientRegistered(account.publicId(), event.userId()));
    }

    private static void requirePermission(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.USERS_MANAGE)) {
            throw new AccessDeniedException("Client management permission required");
        }
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Customer events require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }
}
