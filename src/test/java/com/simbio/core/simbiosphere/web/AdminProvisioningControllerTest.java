package com.simbio.core.simbiosphere.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.identity.domain.PermissionEntity;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.application.AdminProvisioningService;
import com.simbio.core.simbiosphere.application.AdminProvisioningView;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProvisioningControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AdminProvisioningService provisioning;

    @Test
    void adminWithPermissionSeesOnlySanitizedProjection() throws Exception {
        PortalUserPrincipal admin = principal("ADMIN", true);
        AdminProvisioningView row = new AdminProvisioningView(UUID.randomUUID(), UUID.randomUUID(),
                ProvisioningStatus.FAILED, "UNCLASSIFIED", 4, null, Instant.now(), true);
        when(provisioning.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(row));

        String html = mockMvc.perform(get("/gestion/provisioning").with(user(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html).contains("UNCLASSIFIED", "Retry")
                .doesNotContain("REMOTE_SECRET_PAYLOAD");
    }

    @Test
    void employeeIsForbiddenEvenWhenItPresentsPermission() throws Exception {
        mockMvc.perform(get("/gestion/provisioning").with(user(principal("EMPLOYEE", true))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provisioning);
    }

    @Test
    void retryRequiresCsrf() throws Exception {
        mockMvc.perform(post("/gestion/provisioning/" + UUID.randomUUID() + "/retry")
                        .with(user(principal("ADMIN", true))).param("confirmed", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provisioning);
    }

    private static PortalUserPrincipal principal(String role, boolean provisioningPermission) {
        RoleEntity roleEntity = new RoleEntity(role, role);
        UserAccountEntity account = new UserAccountEntity(UUID.randomUUID(), role.toLowerCase() + "@example.test",
                "synthetic-password-hash", "en", Instant.now(), roleEntity);
        if (provisioningPermission) roleEntity.getPermissions().add(
                new PermissionEntity(Permission.PROVISIONING_MANAGE.name(), "Manage provisioning"));
        return new PortalUserPrincipal(account);
    }
}
