package com.simbio.core.identity.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.simbio.core.identity.application.AdminUserActions;
import com.simbio.core.identity.domain.PermissionEntity;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserActions actions;

    @MockitoBean
    private IdentityUserDirectory users;

    @Test
    void administrativeMutationRequiresCsrf() throws Exception {
        TestUser admin = admin(Permission.USERS_MANAGE);
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(post("/gestion/usuarios/{targetId}/estado", targetId)
                        .with(user(admin.principal()))
                        .param("active", "false")
                        .param("confirmed", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(actions, users);
    }

    @Test
    void administrativeMutationRequiresExplicitConfirmation() throws Exception {
        TestUser admin = admin(Permission.USERS_MANAGE);

        mockMvc.perform(post("/gestion/usuarios/{targetId}/desbloquear", UUID.randomUUID())
                        .with(user(admin.principal()))
                        .with(csrf())
                        .param("confirmed", "false"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(actions, users);
    }

    @Test
    void authorizedAdministratorCanDeactivateVisibleAccount() throws Exception {
        TestUser admin = admin(Permission.USERS_MANAGE);
        UUID targetId = UUID.randomUUID();
        when(users.find(admin.actor(), targetId)).thenReturn(Optional.of(identity(targetId, "EMPLOYEE")));

        mockMvc.perform(post("/gestion/usuarios/{targetId}/estado", targetId)
                        .with(user(admin.principal()))
                        .with(csrf())
                        .param("active", "false")
                        .param("confirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestion/usuarios/" + targetId))
                .andExpect(flash().attribute("accountAction", "status"));

        verify(actions).setActive(admin.actor(), targetId, false);
    }

    @Test
    void directActionForUnknownAccountDoesNotInvokeApplicationService() throws Exception {
        TestUser admin = admin(Permission.USERS_MANAGE);
        UUID targetId = UUID.randomUUID();
        when(users.find(admin.actor(), targetId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/gestion/usuarios/{targetId}/restablecer", targetId)
                        .with(user(admin.principal()))
                        .with(csrf())
                        .param("confirmed", "true"))
                .andExpect(status().isNotFound());

        verify(users).find(admin.actor(), targetId);
        verify(actions, never()).sendPasswordReset(admin.actor(), targetId);
    }

    @Test
    void employeeCannotInvokeAdministrativeActions() throws Exception {
        TestUser employee = testUser(UUID.randomUUID(), Role.EMPLOYEE, Permission.USERS_MANAGE);

        mockMvc.perform(post("/gestion/usuarios/{targetId}/estado", UUID.randomUUID())
                        .with(user(employee.principal()))
                        .with(csrf())
                        .param("active", "false")
                        .param("confirmed", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(actions, users);
    }

    private static TestUser admin(Permission... permissions) {
        return testUser(UUID.randomUUID(), Role.ADMIN, permissions);
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
        return new IdentityUserSummary(userId, "target@example.test", "Synthetic target", role,
                "ACTIVE", "en", true, false, Instant.parse("2026-11-19T09:00:00Z"));
    }

    private record TestUser(AuthenticatedUser actor, PortalUserPrincipal principal) {
    }
}
