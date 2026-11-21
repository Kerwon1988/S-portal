package com.simbio.core.simbiosphere.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

class AdminProvisioningServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-21T08:00:00Z");

    @Test
    void employeeOrUnprivilegedAdminCannotList() {
        ProvisioningStore store = mock(ProvisioningStore.class);
        AdminProvisioningService service = service(store, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.search(new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN, Set.of()), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.search(new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.PROVISIONING_MANAGE)), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sanitizesUnknownRemoteErrorBeforeRendering() {
        ProvisioningStore store = mock(ProvisioningStore.class);
        ProvisioningEntity value = processing();
        value.actionRequired("REMOTE_SECRET_PAYLOAD@example.test", NOW);
        when(store.search(null, 100)).thenReturn(List.of(value));

        var rows = service(store, mock(AuditRecorder.class)).search(admin(), null);

        assertThat(rows).singleElement().extracting(AdminProvisioningView::errorCode)
                .isEqualTo("UNCLASSIFIED");
    }

    @Test
    void auditsConfirmedManualRetryButRejectsProvisionedRows() {
        ProvisioningStore store = mock(ProvisioningStore.class);
        AuditRecorder audit = mock(AuditRecorder.class);
        ProvisioningEntity value = processing();
        value.actionRequired("SLUG_TAKEN", NOW);
        when(store.findByPublicId(value.getPublicId())).thenReturn(Optional.of(value));
        AdminProvisioningService service = service(store, audit);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.retry(admin(), value.getPublicId(), true, "correlation-local-1");
            assertThat(value.getStatus()).isEqualTo(ProvisioningStatus.READY);
            verify(audit).record(org.mockito.ArgumentMatchers.argThat(record ->
                    "PROVISIONING_RETRIED".equals(record.actionCode())
                            && value.getPublicId().equals(record.entityPublicId())));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        value.markProcessing("worker", NOW, NOW.plusSeconds(60));
        value.complete("external-account", 10, NOW);
        assertThatThrownBy(() -> service.retry(admin(), value.getPublicId(), true, null))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AdminProvisioningService service(ProvisioningStore store, AuditRecorder audit) {
        return new AdminProvisioningService(store, audit, mock(ApplicationEventPublisher.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AuthenticatedUser admin() {
        return new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN, Set.of(Permission.PROVISIONING_MANAGE));
    }

    private static ProvisioningEntity processing() {
        ProvisioningEntity value = new ProvisioningEntity(UUID.randomUUID(), UUID.randomUUID(),
                "SIMBIOSPHERE", UUID.randomUUID().toString(), NOW);
        value.attachIntent(UUID.randomUUID(), "Synthetic shop", "synthetic-shop", "SERVICES",
                "Main", "main", "[]", "SIM-TERMS-1", NOW);
        value.markReady(NOW);
        value.markProcessing("worker", NOW, NOW.plusSeconds(60));
        return value;
    }
}
