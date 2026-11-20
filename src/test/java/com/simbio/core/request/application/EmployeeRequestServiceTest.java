package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.api.StaffDirectory;

class EmployeeRequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private final RequestStore store = org.mockito.Mockito.mock(RequestStore.class);
    private final StaffDirectory staff = org.mockito.Mockito.mock(StaffDirectory.class);
    private final ConversationApi conversations = org.mockito.Mockito.mock(ConversationApi.class);
    private final FileStorage files = org.mockito.Mockito.mock(FileStorage.class);
    private final AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final EmployeeRequestService service =     new EmployeeRequestService(store, staff, conversations, files,
            audit, events, 1, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void oneBusinessDaySlaSkipsWeekend() {
        assertThat(EmployeeRequestService.addBusinessDays(LocalDate.of(2026, 11, 20), 1))
                .isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void rejectsClientAndUnprivilegedEmployee() {
        var client = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        var employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.dashboard(client)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.dashboard(employee)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsOperationalUpdateFromUnassignedEmployee() {
        var actor = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of(Permission.REQUESTS_VIEW_ALL));
        RequestEntity request = request();
        when(store.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));
        when(store.isAssigned(request.getId(), actor.userId())).thenReturn(false);

        assertThatThrownBy(() -> service.update(actor, request.getPublicId(), null, "HIGH",
                BigDecimal.ONE, null, null)).isInstanceOf(AccessDeniedException.class);
    }

    private static RequestEntity request() {
        var value = new RequestEntity(UUID.randomUUID(), "SOL-2026-000001", RequestType.CUSTOM_DEVELOPMENT,
                "WEB_APPLICATION", "Synthetic", "Synthetic", null, null, null, null, null,
                UUID.randomUUID(), UUID.randomUUID(), NOW.minusSeconds(60));
        try {
            var field = RequestEntity.class.getDeclaredField("id");
            field.setAccessible(true); field.set(value, 1L);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        return value;
    }
}
