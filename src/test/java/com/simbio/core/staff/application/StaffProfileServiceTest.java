package com.simbio.core.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.identity.api.StaffAccountAccessManager;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.domain.StaffProfileEntity;

class StaffProfileServiceTest {

    private final StaffProfileStore store = Mockito.mock(StaffProfileStore.class);
    private final AuditRecorder auditRecorder = Mockito.mock(AuditRecorder.class);

    @Test
    void acceptedEmployeeInvitationCreatesActiveProfileWithEmptySpecialties() {
        UUID userId = UUID.randomUUID();
        Instant acceptedAt = Instant.parse("2026-11-18T12:00:00Z");
        StaffProfileService service = new StaffProfileService(store, auditRecorder,
                Mockito.mock(StaffAccountAccessManager.class),
                Clock.fixed(acceptedAt, ZoneOffset.UTC));
        StaffProfileEntity savedProfile = new StaffProfileEntity(
                userId, LocalDate.of(2026, 11, 18), acceptedAt);
        when(store.find(userId)).thenReturn(Optional.empty(), Optional.of(savedProfile));
        when(store.save(any(StaffProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.specialties(userId)).thenReturn(List.of());

        service.createFromInvitation(new InvitationAccepted(userId, "EMPLOYEE"));

        ArgumentCaptor<StaffProfileEntity> profile = ArgumentCaptor.forClass(StaffProfileEntity.class);
        verify(store).save(profile.capture());
        assertThat(profile.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getValue().getEmploymentStartDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(service.findByUserId(userId).orElseThrow().specialties()).isEmpty();
    }

    @Test
    void ignoresClientInvitationsAndDoesNotDuplicateEmployeeProfiles() {
        UUID userId = UUID.randomUUID();
        StaffProfileService service = new StaffProfileService(store, auditRecorder,
                Mockito.mock(StaffAccountAccessManager.class));
        when(store.find(userId)).thenReturn(Optional.of(
                new StaffProfileEntity(userId, LocalDate.of(2026, 11, 18), Instant.now())));

        service.createFromInvitation(new InvitationAccepted(userId, "CLIENT"));
        service.createFromInvitation(new InvitationAccepted(userId, "EMPLOYEE"));

        verify(store, never()).save(any(StaffProfileEntity.class));
    }

    @Test
    void synchronizesAdminBootstrapIntoAnActiveStaffProfile() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-18T12:00:00Z");
        StaffProfileService service = new StaffProfileService(store, auditRecorder,
                Mockito.mock(StaffAccountAccessManager.class), Clock.fixed(now, ZoneOffset.UTC));
        when(store.find(userId)).thenReturn(Optional.empty());
        when(store.save(any(StaffProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.synchronizeIdentityAccount(new IdentityAccountChanged(userId, "ADMIN", "ACTIVE"));

        ArgumentCaptor<StaffProfileEntity> profile = ArgumentCaptor.forClass(StaffProfileEntity.class);
        verify(store).save(profile.capture());
        assertThat(profile.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getValue().getEmploymentStartDate()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void rejectsStaffUpdatesWithoutAdminPermission() {
        UUID userId = UUID.randomUUID();
        StaffProfileService service = new StaffProfileService(store, auditRecorder,
                Mockito.mock(StaffAccountAccessManager.class));
        AuthenticatedUser employee = new AuthenticatedUser(userId, Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.update(employee, userId, "Specialist", "INACTIVE", List.of()))
                .isInstanceOf(AccessDeniedException.class);

        verify(store, never()).find(userId);
        verify(store, never()).save(any(StaffProfileEntity.class));
    }

    @Test
    void adminUpdateNormalizesAndAuditsSpecialtiesAndStatus() {
        UUID actorId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        StaffProfileEntity profile = new StaffProfileEntity(
                workerId, LocalDate.of(2026, 11, 18), Instant.parse("2026-11-18T12:00:00Z"));
        StaffAccountAccessManager accountAccess = Mockito.mock(StaffAccountAccessManager.class);
        StaffProfileService service = new StaffProfileService(store, auditRecorder, accountAccess);
        when(store.find(workerId)).thenReturn(Optional.of(profile));
        when(store.save(profile)).thenReturn(profile);
        AuthenticatedUser admin = new AuthenticatedUser(actorId, Role.ADMIN, Set.of(Permission.STAFF_MANAGE));

        var result = service.update(admin, workerId, "  Specialist  ", "INACTIVE",
                List.of("Analysis", "analysis", "Delivery"));

        assertThat(result.jobTitle()).isEqualTo("Specialist");
        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(result.specialties()).containsExactly("Analysis", "Delivery");
        verify(store).replaceSpecialties(workerId, List.of("Analysis", "Delivery"));
        verify(accountAccess).setActive(admin, workerId, false);
        ArgumentCaptor<AuditRecord> auditRecords = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder, Mockito.times(2)).record(auditRecords.capture());
        assertThat(auditRecords.getAllValues()).extracting(AuditRecord::actionCode)
                .containsExactly("STAFF_PROFILE_UPDATED", "STAFF_STATUS_CHANGED");
    }
}
