package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class RequestTransitionServiceTest {
    private final RequestStore store = org.mockito.Mockito.mock(RequestStore.class);
    private final AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final RequestTransitionService service = new RequestTransitionService(store, audit, events,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Test
    void clientCannotChangeRequestState() {
        var client = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        assertThatThrownBy(() -> service.transition(client, UUID.randomUUID(), RequestStatus.EN_ANALISIS, null, null))
                .isInstanceOf(AccessDeniedException.class);
        verify(store, never()).findByPublicId(any());
    }

    @Test
    void authorizedEmployeeCanMoveReceivedToAnalysis() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            var employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                    Set.of(Permission.REQUESTS_VIEW_ALL));
            RequestEntity request = new RequestEntity(UUID.randomUUID(), "SOL-2026-000010",
                    RequestType.CUSTOM_DEVELOPMENT, "WEB_APPLICATION", "Title", "Description",
                    null, null, null, null, null, UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
            var id = RequestEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(request, 1L);
            when(store.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));

            service.transition(employee, request.getPublicId(), RequestStatus.EN_ANALISIS, null, null);

            verify(store).save(request);
            verify(store).saveHistory(any());
            verify(events, never()).publishEvent(any(Object.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
