package com.simbio.core.simbiosphere.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.simbiosphere.api.ProvisioningStateChanged;
import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@Service
public class AdminProvisioningService {
    private static final Set<String> ALLOWED_ERROR_CODES = Set.of(
            "SLUG_TAKEN", "EMAIL_ALREADY_REGISTERED", "INVALID_MODULES", "UPSTREAM_TIMEOUT",
            "DEPENDENCY_UNAVAILABLE", "TRANSPORT_UNAVAILABLE", "CIRCUIT_OPEN", "OWNER_VERIFICATION_UNAVAILABLE");

    private final ProvisioningStore store;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public AdminProvisioningService(ProvisioningStore store, AuditRecorder audit,
            ApplicationEventPublisher events) {
        this(store, audit, events, Clock.systemUTC());
    }

    AdminProvisioningService(ProvisioningStore store, AuditRecorder audit,
            ApplicationEventPublisher events, Clock clock) {
        this.store = store;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminProvisioningView> search(AuthenticatedUser actor, ProvisioningStatus status) {
        requireAdmin(actor);
        return store.search(status, 100).stream().map(AdminProvisioningService::view).toList();
    }

    @Transactional
    public void retry(AuthenticatedUser actor, UUID provisioningId, boolean confirmed, String correlationId) {
        requireAdmin(actor);
        if (!confirmed) throw new IllegalArgumentException("Confirmation is required");
        ProvisioningEntity value = store.findByPublicId(provisioningId)
                .orElseThrow(() -> new IllegalArgumentException("Provisioning not found"));
        if (value.getStatus() == ProvisioningStatus.PROVISIONED
                || value.getStatus() == ProvisioningStatus.PROCESSING
                || value.getStatus() == ProvisioningStatus.READY) {
            throw new IllegalStateException("Provisioning is not retryable");
        }
        Instant now = Instant.now(clock);
        value.retry(value.getErrorCode() == null ? "MANUAL_RETRY" : value.getErrorCode(), now, now);
        store.save(value);
        audit.record(new AuditRecord(actor.userId(), "PROVISIONING_RETRIED", "PROVISIONING",
                value.getPublicId(), safeCorrelation(correlationId)));
        publishAfterCommit(value, now);
    }

    private void publishAfterCommit(ProvisioningEntity value, Instant now) {
        ProvisioningStateChanged event = new ProvisioningStateChanged(value.getPublicId(),
                value.getClientAccountPublicId(), value.getStatus(), now);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }

    private static AdminProvisioningView view(ProvisioningEntity value) {
        String error = value.getErrorCode();
        return new AdminProvisioningView(value.getPublicId(), value.getClientAccountPublicId(),
                value.getStatus(), ALLOWED_ERROR_CODES.contains(error) ? error : "UNCLASSIFIED",
                value.getAttemptCount(), value.getNextAttemptAt(), value.getUpdatedAt(),
                value.getStatus() == ProvisioningStatus.FAILED
                        || value.getStatus() == ProvisioningStatus.ACTION_REQUIRED);
    }

    private static void requireAdmin(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN
                || !actor.hasPermission(Permission.PROVISIONING_MANAGE)) {
            throw new AccessDeniedException("PROVISIONING_MANAGE is required");
        }
    }

    private static String safeCorrelation(String value) {
        return value == null || !value.matches("[A-Za-z0-9._:-]{1,64}") ? null : value;
    }
}
