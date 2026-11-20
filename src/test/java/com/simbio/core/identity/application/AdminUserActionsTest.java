package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class AdminUserActionsTest {

    private final UserAccountStore accounts = org.mockito.Mockito.mock(UserAccountStore.class);
    private final PasswordRecoveryService passwordRecovery = org.mockito.Mockito.mock(PasswordRecoveryService.class);
    private final InvitationIssuer invitations = org.mockito.Mockito.mock(InvitationIssuer.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final SessionInvalidator sessions = org.mockito.Mockito.mock(SessionInvalidator.class);
    private final EmailSender emailSender = org.mockito.Mockito.mock(EmailSender.class);
    private final AdminUserActions actions = new AdminUserActions(
            accounts, passwordRecovery, invitations, auditRecorder, events, sessions, emailSender);

    @Test
    void refusesToDeactivateTheLastActiveAdministrator() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserAccountEntity target = account(targetId, "ADMIN");
        when(accounts.findByPublicId(targetId)).thenReturn(java.util.Optional.of(target));
        when(accounts.lockActiveAdmins()).thenReturn(java.util.List.of(target));

        assertThatThrownBy(() -> actions.setActive(admin(actorId, Permission.USERS_MANAGE), targetId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last active administrator");

        verify(accounts, never()).save(target);
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesAnAdministratorFromDemotingTheirOwnAccount() {
        UUID adminId = UUID.randomUUID();
        UserAccountEntity target = account(adminId, "ADMIN");
        when(accounts.findByPublicId(adminId)).thenReturn(java.util.Optional.of(target));
        when(accounts.lockActiveAdmins()).thenReturn(java.util.List.of(
                target, account(UUID.randomUUID(), "ADMIN")));

        assertThatThrownBy(() -> actions.changeRole(
                admin(adminId, Permission.USERS_MANAGE, Permission.STAFF_MANAGE), adminId, "EMPLOYEE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demote themselves");

        verify(accounts, never()).save(target);
    }

    @Test
    void activationIsPersistedAuditedAndNotified() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = account(employeeId, "EMPLOYEE");
        employee.setActive(false);
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));

        actions.setActive(admin(actorId, Permission.USERS_MANAGE), employeeId, true);

        verify(accounts).save(employee);
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(record ->
                actorId.equals(record.actorId())
                        && "USER_ACTIVATED".equals(record.actionCode())
                        && employeeId.equals(record.entityPublicId())));
        verify(emailSender).enqueue(org.mockito.ArgumentMatchers.argThat(command ->
                command.recipientUserId().equals(employeeId)
                        && command.template() == EmailTemplate.ACCOUNT_SECURITY_CHANGED));
        verify(events).publishEvent(new IdentityAccountChanged(employeeId, "EMPLOYEE", "ACTIVE"));
    }

    @Test
    void publishesIdentityChangeWhenAnEmployeeIsPromotedToAdministrator() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = account(employeeId, "EMPLOYEE");
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(accounts.findRole("ADMIN")).thenReturn(new RoleEntity("ADMIN", "Administrator"));

        actions.changeRole(
                admin(actorId, Permission.USERS_MANAGE, Permission.STAFF_MANAGE), employeeId, "ADMIN");

        verify(accounts).save(employee);
        verify(events).publishEvent(new IdentityAccountChanged(employeeId, "ADMIN", "ACTIVE"));
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.any());
        verify(emailSender).enqueue(org.mockito.ArgumentMatchers.argThat(command ->
                command.recipientUserId().equals(employeeId)
                        && command.template() == EmailTemplate.ACCOUNT_SECURITY_CHANGED));
    }

    @Test
    void unlockClearsLoginLockPersistsAuditsAndNotifies() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = account(employeeId, "EMPLOYEE");
        Instant failedLogin = Instant.parse("2026-11-18T12:00:00Z");
        for (int attempt = 0; attempt < 5; attempt++) {
            employee.recordFailedLogin(failedLogin.plusSeconds(attempt));
        }
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));

        actions.unlock(admin(actorId, Permission.USERS_MANAGE), employeeId);

        org.assertj.core.api.Assertions.assertThat(employee.getLockedUntil()).isNull();
        org.assertj.core.api.Assertions.assertThat(employee.getFailedLoginCount()).isZero();
        verify(accounts).save(employee);
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(record ->
                actorId.equals(record.actorId())
                        && "USER_UNLOCKED".equals(record.actionCode())
                        && employeeId.equals(record.entityPublicId())));
        verify(emailSender).enqueue(org.mockito.ArgumentMatchers.argThat(command ->
                command.recipientUserId().equals(employeeId)
                        && command.template() == EmailTemplate.ACCOUNT_SECURITY_CHANGED));
    }

    @Test
    void administrativePasswordResetDelegatesToRecoveryFlowAndAuditsTheRequest() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = account(employeeId, "EMPLOYEE");
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));

        actions.sendPasswordReset(admin(actorId, Permission.USERS_MANAGE), employeeId);

        verify(passwordRecovery).requestReset("employee@example.test");
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(record ->
                actorId.equals(record.actorId())
                        && "PASSWORD_RESET_REQUESTED".equals(record.actionCode())
                        && employeeId.equals(record.entityPublicId())));
        verifyNoInteractions(emailSender);
    }

    @Test
    void refusesRoleChangesThatWouldOrphanCustomerAccounts() {
        UUID actorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UserAccountEntity client = account(clientId, "CLIENT");
        when(accounts.findByPublicId(clientId)).thenReturn(java.util.Optional.of(client));

        assertThatThrownBy(() -> actions.changeRole(
                admin(actorId, Permission.USERS_MANAGE, Permission.STAFF_MANAGE), clientId, "EMPLOYEE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer-account workflow");

        verify(accounts, never()).save(client);
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesPromotingAnEmployeeToClientWithoutCustomerAccountWorkflow() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UserAccountEntity employee = account(employeeId, "EMPLOYEE");
        when(accounts.findByPublicId(employeeId)).thenReturn(java.util.Optional.of(employee));

        assertThatThrownBy(() -> actions.changeRole(
                admin(actorId, Permission.USERS_MANAGE, Permission.STAFF_MANAGE), employeeId, "CLIENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer-account workflow");

        verify(accounts, never()).save(employee);
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
        verify(auditRecorder, never()).record(org.mockito.ArgumentMatchers.any());
        verify(emailSender, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void requiresUserManagementPermissionBeforeLookingUpAnAccount() {
        UUID targetId = UUID.randomUUID();
        AuthenticatedUser staffAdmin = admin(UUID.randomUUID(), Permission.STAFF_MANAGE);

        assertThatThrownBy(() -> actions.unlock(staffAdmin, targetId))
                .isInstanceOf(AccessDeniedException.class);

        verify(accounts, never()).findByPublicId(targetId);
    }

    private static UserAccountEntity account(UUID userId, String roleCode) {
        return new UserAccountEntity(userId, roleCode.toLowerCase() + "@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity(roleCode, roleCode));
    }

    private static AuthenticatedUser admin(UUID userId, Permission... permissions) {
        return new AuthenticatedUser(userId, Role.ADMIN, Set.of(permissions));
    }
}
