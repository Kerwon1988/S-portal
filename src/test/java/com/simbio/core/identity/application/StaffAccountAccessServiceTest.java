package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class StaffAccountAccessServiceTest {

    private final UserAccountStore accounts = org.mockito.Mockito.mock(UserAccountStore.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final SessionInvalidator sessions = org.mockito.Mockito.mock(SessionInvalidator.class);
    private final StaffAccountAccessService service = new StaffAccountAccessService(accounts, auditRecorder, sessions);

    @Test
    void synchronizesOnlyAuthorizedEmployeeAccessAndAuditsTheAccountChange() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = new UserAccountEntity(employeeId, "employee@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("EMPLOYEE", "Employee"));
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));
        AuthenticatedUser admin = new AuthenticatedUser(actorId, Role.ADMIN, Set.of(Permission.STAFF_MANAGE));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.setActive(admin, employeeId, false);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        org.assertj.core.api.Assertions.assertThat(employee.getStatus()).isEqualTo("INACTIVE");
        verify(accounts).save(employee);
        verify(auditRecorder).record(any());
        verify(sessions).invalidate(employeeId);
    }

    @Test
    void deniesEmployeesAndDoesNotMutateTheIdentityAccount() {
        UUID employeeId = UUID.randomUUID();
        AuthenticatedUser employee = new AuthenticatedUser(employeeId, Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.setActive(employee, employeeId, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(accounts, never()).findByPublicId(employeeId);
        verify(accounts, never()).save(any(UserAccountEntity.class));
    }

    @Test
    void refusesToChangeClientAccountsThroughStaffManagement() {
        UUID actorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UserAccountEntity client = new UserAccountEntity(clientId, "client@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("CLIENT", "Client"));
        when(accounts.findByPublicId(clientId)).thenReturn(java.util.Optional.of(client));
        AuthenticatedUser admin = new AuthenticatedUser(actorId, Role.ADMIN, Set.of(Permission.STAFF_MANAGE));

        assertThatThrownBy(() -> service.setActive(admin, clientId, false))
                .isInstanceOf(IllegalArgumentException.class);

        verify(accounts, never()).save(any(UserAccountEntity.class));
    }

    @Test
    void refusesToDeactivateTheLastActiveAdministrator() {
        UUID actorId = UUID.randomUUID();
        UUID otherAdminId = UUID.randomUUID();
        UserAccountEntity otherAdmin = new UserAccountEntity(otherAdminId, "admin@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("ADMIN", "Administrator"));
        when(accounts.findByPublicId(otherAdminId)).thenReturn(java.util.Optional.of(otherAdmin));
        when(accounts.lockActiveAdmins()).thenReturn(java.util.List.of(otherAdmin));
        AuthenticatedUser admin = new AuthenticatedUser(actorId, Role.ADMIN, Set.of(Permission.STAFF_MANAGE));

        assertThatThrownBy(() -> service.setActive(admin, otherAdminId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last active administrator");

        verify(accounts, never()).save(any(UserAccountEntity.class));
        verify(auditRecorder, never()).record(any());
        verify(sessions, never()).invalidate(any());
    }

    @Test
    void refusesAdministratorsFromDeactivatingTheirOwnAccount() {
        UUID adminId = UUID.randomUUID();
        UserAccountEntity administrator = new UserAccountEntity(adminId, "admin@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("ADMIN", "Administrator"));
        when(accounts.findByPublicId(adminId)).thenReturn(java.util.Optional.of(administrator));
        when(accounts.lockActiveAdmins()).thenReturn(java.util.List.of(administrator));
        AuthenticatedUser admin = new AuthenticatedUser(adminId, Role.ADMIN, Set.of(Permission.STAFF_MANAGE));

        assertThatThrownBy(() -> service.setActive(admin, adminId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own account");

        verify(accounts, never()).save(any(UserAccountEntity.class));
        verify(auditRecorder, never()).record(any());
        verify(sessions, never()).invalidate(any());
    }
}
