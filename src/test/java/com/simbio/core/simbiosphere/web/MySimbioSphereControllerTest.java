package com.simbio.core.simbiosphere.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.simbiosphere.application.ProvisioningService;
import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.api.ProvisioningSummary;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MySimbioSphereControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProvisioningService provisioning;
    @MockitoBean com.simbio.core.simbiosphere.application.DemoProvisioningClient demoClient;

    @Test
    void ownerSeesOnlyTheirProvisioningSummary() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");
        when(provisioning.ownedBy(owner.authenticatedUser().userId())).thenReturn(Optional.of(
                new ProvisioningSummary(UUID.randomUUID(), UUID.randomUUID(), ProvisioningStatus.PROVISIONED,
                        null, 1, Instant.now(), Instant.now())));

        String html = mockMvc.perform(get("/cuenta/simbiosphere").with(user(owner))
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html)
                .contains("My SimbioSphere", "Provisioned")
                .doesNotContain("synthetic-tenant-secret");
    }

    @Test
    void employeeCannotOpenClientPanel() throws Exception {
        mockMvc.perform(get("/cuenta/simbiosphere").with(user(principal("EMPLOYEE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provisioning);
    }

    private static PortalUserPrincipal principal(String role) {
        RoleEntity roleEntity = new RoleEntity(role, role);
        UserAccountEntity account = new UserAccountEntity(UUID.randomUUID(), role.toLowerCase() + "@example.test",
                "synthetic-password-hash", "en", Instant.now(), roleEntity);
        return new PortalUserPrincipal(account);
    }
}
