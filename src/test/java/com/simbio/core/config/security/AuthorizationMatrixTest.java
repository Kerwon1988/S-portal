package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyKnownAuditRouteRequiresItsPublishedPermission() throws Exception {
        assertThat(AuthorizationRules.routes())
                .allSatisfy(rule -> assertThat(rule.permission()).isEqualTo("AUDIT_VIEW"));

        mockMvc.perform(get("/gestion/auditoria"))
                .andExpect(status().isForbidden());
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
    void routeRulesAreReadOnlyAuditEndpoints() {
        assertThat(AuthorizationRules.routes()).allSatisfy(rule ->
                assertThat(rule.method()).isEqualTo(HttpMethod.GET));
    }
}
