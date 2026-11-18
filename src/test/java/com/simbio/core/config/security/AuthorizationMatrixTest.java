package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @BeforeEach
    void provideCurrentUser() {
        when(currentUser.currentUser()).thenReturn(Optional.of(
                new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT))));
    }

    @Test
    void everyKnownAuditRouteRequiresItsPublishedPermission() throws Exception {
        assertThat(AuthorizationRules.routes().stream()
                .filter(rule -> rule.path().startsWith("/gestion/auditoria")).toList())
                .allSatisfy(rule -> assertThat(rule.permission()).isEqualTo("AUDIT_VIEW"));

        mockMvc.perform(get("/gestion/auditoria"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/auditoria/usuarios/{userId}/panel", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/auditoria/usuarios/{userId}/panel", UUID.randomUUID())
                        .with(user("employee").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "AUDIT_VIEW"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/gestion/auditoria")
                        .with(user("employee").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "AUDIT_VIEW"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/gestion/auditoria")
                        .with(user("client").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_CLIENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void contentManagementRequiresItsPermissionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/gestion/contenido")
                        .with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/contenido/nuevo")
                        .with(user("editor").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "CONTENT_EDIT"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/gestion/contenido/legal")
                        .with(user("editor").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "CONTENT_EDIT")))
                        .param("documentType", "PRIVACY")
                        .param("locale", "es")
                        .param("title", "Privacidad")
                        .param("contentMarkdown", "Texto de prueba"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnclassifiedRoutesAndMutationsWithoutCsrf() throws Exception {
        mockMvc.perform(get("/gestion/not-classified")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/acceso")
                        .param("username", "synthetic@example.test")
                        .param("password", "synthetic-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void routeRulesClassifyAuditAndContentOperations() {
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().startsWith("/gestion/auditoria"))
                .allSatisfy(rule -> {
                    assertThat(rule.method()).isEqualTo(HttpMethod.GET);
                    assertThat(rule.permission()).isEqualTo("AUDIT_VIEW");
                });
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().startsWith("/gestion/contenido"))
                .allSatisfy(rule -> assertThat(rule.permission()).isEqualTo("CONTENT_EDIT"));
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().startsWith("/gestion/contenido"))
                .extracting(AuthorizationRules.RoutePermission::method)
                .contains(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    void staffAndUserManagementRoutesRequireTheirDedicatedAdminPermissions() throws Exception {
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().equals("/gestion/clientes")
                        || rule.path().equals("/gestion/trabajadores"))
                .allSatisfy(rule -> {
                    assertThat(rule.method()).isEqualTo(HttpMethod.GET);
                    assertThat(rule.permission()).isEqualTo("USERS_MANAGE");
                    assertThat(rule.role()).isEqualTo("ADMIN");
                });
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().equals("/gestion/clientes/export.csv"))
                .allSatisfy(rule -> {
                    assertThat(rule.permission()).isEqualTo("CLIENTS_EXPORT");
                    assertThat(rule.role()).isEqualTo("ADMIN");
                });
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().contains("/gestion/trabajadores/"))
                .allSatisfy(rule -> {
                    assertThat(rule.permission()).isEqualTo("STAFF_MANAGE");
                    assertThat(rule.role()).isEqualTo("ADMIN");
                });
        assertThat(AuthorizationRules.routes())
                .filteredOn(rule -> rule.path().equals("/gestion/usuarios/**"))
                .extracting(AuthorizationRules.RoutePermission::method)
                .contains(HttpMethod.GET, HttpMethod.POST);

        mockMvc.perform(get("/gestion/clientes")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/trabajadores/invitacion")
                        .with(user("admin").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("USERS_MANAGE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestion/usuarios/00000000-0000-0000-0000-000000000001/estado")
                        .with(user("admin").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("USERS_MANAGE")))
                        .param("active", "false")
                        .param("confirmed", "true"))
                .andExpect(status().isForbidden());
    }
}
