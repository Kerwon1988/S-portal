package com.simbio.core.identity.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;

class AdminBootstrapServiceTest {

    private final UserAccountStore accounts = org.mockito.Mockito.mock(UserAccountStore.class);
    private final BootstrapStateStore bootstrapState = org.mockito.Mockito.mock(BootstrapStateStore.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final AdminBootstrapService service = new AdminBootstrapService(accounts, bootstrapState,
            new PasswordPolicy(), passwordEncoder, auditRecorder, events,
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void publishesAccountChangeForStaffProfileSynchronization() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity administrator = new UserAccountEntity(userId, "admin@example.test", "encoded", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("ADMIN", "Administrator"));
        when(bootstrapState.wasConsumed()).thenReturn(false);
        when(accounts.existsAdmin()).thenReturn(false);
        when(passwordEncoder.encode("long-enough-password")).thenReturn("encoded");
        when(accounts.createAdmin("admin@example.test", "encoded", "en")).thenReturn(administrator);

        boolean created = service.bootstrap("admin@example.test", "long-enough-password", "en");

        org.assertj.core.api.Assertions.assertThat(created).isTrue();
        verify(events).publishEvent(new IdentityAccountChanged(userId, "ADMIN", "ACTIVE"));
        verify(bootstrapState).consume(Instant.parse("2026-11-18T12:00:00Z"));
    }
}
