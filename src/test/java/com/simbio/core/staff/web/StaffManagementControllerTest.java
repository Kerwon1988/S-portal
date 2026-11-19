package com.simbio.core.staff.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.domain.PermissionEntity;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.api.StaffSummary;
import com.simbio.core.staff.application.StaffProfileService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffProfileService profiles;

    @MockitoBean
    private IdentityUserDirectory identityUsers;

    @MockitoBean
    private InvitationService invitations;

    @Test
    void workerInvitationRequiresAdminPermissionAndCsrf() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);

        mockMvc.perform(post("/gestion/trabajadores/invitacion")
                        .with(user(admin.principal()))
                        .param("email", "worker@example.test"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/gestion/trabajadores/invitacion")
                        .with(user(admin(Permission.USERS_MANAGE).principal())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(invitations);
    }

    @Test
    void authorizedAdministratorCanInviteWorkerThroughIdentityApi() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);
        when(invitations.invite(admin.actor(), "worker@example.test", "EMPLOYEE", java.util.Locale.ENGLISH))
                .thenReturn(true);

        mockMvc.perform(post("/gestion/trabajadores/invitacion")
                        .with(user(admin.principal()))
                        .with(csrf())
                        .header("Accept-Language", "en")
                        .param("email", "worker@example.test"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/invitation"))
                .andExpect(model().attribute("sent", true));

        verify(invitations).invite(admin.actor(), "worker@example.test", "EMPLOYEE", java.util.Locale.ENGLISH);
    }

    @Test
    void employeeCannotEditWorkerProfileEvenWithStaffPermissionAuthority() throws Exception {
        TestUser employee = employee(Permission.STAFF_MANAGE);

        mockMvc.perform(post("/gestion/trabajadores/" + UUID.randomUUID())
                        .with(user(employee.principal()))
                        .with(csrf())
                        .param("jobTitle", "Synthetic role")
                        .param("status", "INACTIVE")
                        .param("specialties", "Synthetic specialty"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profiles, identityUsers);
    }

    @Test
    void profileUpdateRequiresCsrf() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);

        mockMvc.perform(post("/gestion/trabajadores/{workerId}", UUID.randomUUID())
                        .with(user(admin.principal()))
                        .param("jobTitle", "Analyst")
                        .param("status", "ACTIVE")
                        .param("specialties", "Analysis"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profiles, identityUsers);
    }

    @Test
    void profileUpdateIsScopedToAnExistingEmployeeAndAuditedByTheOwnerService() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);
        UUID workerId = UUID.randomUUID();
        IdentityUserSummary identity = identity(workerId, "EMPLOYEE");
        StaffSummary existing = staff(workerId, "ACTIVE", List.of());
        StaffSummary updated = staff(workerId, "ACTIVE", List.of("Analysis", "Delivery"));
        when(identityUsers.find(admin.actor(), workerId)).thenReturn(Optional.of(identity));
        when(profiles.findByUserId(workerId)).thenReturn(Optional.of(existing));
        when(profiles.update(admin.actor(), workerId, "Analyst", "ACTIVE",
                List.of("Analysis", "Delivery"))).thenReturn(updated);

        mockMvc.perform(post("/gestion/trabajadores/{workerId}", workerId)
                        .with(user(admin.principal()))
                        .with(csrf())
                        .param("jobTitle", "Analyst")
                        .param("status", "ACTIVE")
                        .param("specialties", "Analysis\nDelivery"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/detail"))
                .andExpect(model().attribute("saved", true));

        verify(profiles).update(admin.actor(), workerId, "Analyst", "ACTIVE",
                List.of("Analysis", "Delivery"));
    }

    @Test
    void directProfileLookupDoesNotRevealNonStaffAccounts() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);
        UUID clientId = UUID.randomUUID();
        when(identityUsers.find(admin.actor(), clientId)).thenReturn(Optional.of(identity(clientId, "CLIENT")));

        mockMvc.perform(get("/gestion/trabajadores/{workerId}", clientId)
                        .with(user(admin.principal())))
                .andExpect(status().isNotFound());

        verify(profiles, never()).findByUserId(clientId);
    }

    @Test
    void invalidProfileFormDoesNotCallUpdate() throws Exception {
        TestUser admin = admin(Permission.STAFF_MANAGE);
        UUID workerId = UUID.randomUUID();
        when(identityUsers.find(admin.actor(), workerId))
                .thenReturn(Optional.of(identity(workerId, "EMPLOYEE")));
        when(profiles.findByUserId(workerId)).thenReturn(Optional.of(staff(workerId, "ACTIVE", List.of())));

        mockMvc.perform(post("/gestion/trabajadores/{workerId}", workerId)
                        .with(user(admin.principal()))
                        .with(csrf())
                        .param("jobTitle", "x".repeat(121))
                        .param("status", "ACTIVE")
                        .param("specialties", "Analysis"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "jobTitle"));

        verify(profiles, never()).update(eq(admin.actor()), eq(workerId), eq("x".repeat(121)),
                eq("ACTIVE"), eq(List.of("Analysis")));
    }

    private static TestUser admin(Permission... permissions) {
        return testUser(UUID.randomUUID(), Role.ADMIN, permissions);
    }

    private static TestUser employee(Permission... permissions) {
        return testUser(UUID.randomUUID(), Role.EMPLOYEE, permissions);
    }

    private static TestUser testUser(UUID userId, Role role, Permission... permissions) {
        RoleEntity roleEntity = new RoleEntity(role.name(), role.name());
        Set<Permission> permissionSet = Set.of(permissions);
        permissionSet.forEach(permission -> roleEntity.getPermissions().add(
                new PermissionEntity(permission.name(), permission.name())));
        UserAccountEntity account = new UserAccountEntity(userId, role.name().toLowerCase() + "@example.test",
                "synthetic-password-hash", "en", Instant.parse("2026-11-19T09:00:00Z"), roleEntity);
        return new TestUser(new AuthenticatedUser(userId, role, permissionSet), new PortalUserPrincipal(account));
    }

    private static IdentityUserSummary identity(UUID userId, String role) {
        return new IdentityUserSummary(userId, "user@example.test", "Synthetic user", role,
                "ACTIVE", "en", true, false, Instant.parse("2026-11-19T09:00:00Z"));
    }

    private static StaffSummary staff(UUID userId, String status, List<String> specialties) {
        return new StaffSummary(userId, "Analyst", LocalDate.of(2026, 11, 19), status,
                specialties, Instant.parse("2026-11-19T09:00:00Z"));
    }

    private record TestUser(AuthenticatedUser actor, PortalUserPrincipal principal) {
    }
}
