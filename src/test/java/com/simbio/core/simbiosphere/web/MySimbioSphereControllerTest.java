package com.simbio.core.simbiosphere.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void ownerChecksSlugWithoutExposingProviderDetails() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");
        when(demoClient.checkSlug("synthetic-shop")).thenReturn(
                new com.simbio.core.simbiosphere.application.DemoProvisioningClient.SlugAvailability(
                        "synthetic-shop", true, java.util.List.of()));

        String html = mockMvc.perform(get("/cuenta/simbiosphere/slug")
                        .param("businessSlug", "synthetic-shop").with(user(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html).contains("simbiosphere").doesNotContain("synthetic-secret");
    }

    @Test
    void verifiedOwnerStartsJointIntentWithSeparateTermsAndCsrf() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");
        when(provisioning.ownedBy(owner.authenticatedUser().userId())).thenReturn(Optional.empty());

        mockMvc.perform(post("/cuenta/simbiosphere").with(user(owner)).with(csrf())
                        .param("businessName", "Synthetic shop")
                        .param("businessSlug", "synthetic-shop")
                        .param("businessType", "SERVICES")
                        .param("locationName", "Main")
                        .param("locationSlug", "main")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection());

        verify(provisioning).begin(org.mockito.ArgumentMatchers.eq(owner.authenticatedUser().userId()),
                org.mockito.ArgumentMatchers.argThat(intent ->
                        "SIM-TERMS-1".equals(intent.termsVersion())
                                && intent.termsAcceptedAt() != null));
    }

    @Test
    void ownerCorrectsSlugWithCsrfWhenProviderRequiresAction() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");

        mockMvc.perform(post("/cuenta/simbiosphere/correccion-slug").with(user(owner)).with(csrf())
                        .param("businessSlug", "synthetic-shop-new"))
                .andExpect(status().is3xxRedirection());
        verify(provisioning).correctActionRequired(owner.authenticatedUser().userId(), "synthetic-shop-new");
    }

    @Test
    void provisionedOwnerGetsFreshActivationRedirectWithoutPersistingToken() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");
        java.net.URI activation = java.net.URI.create("https://orders.simbio.info/activate/synthetic-token");
        when(provisioning.activationLink(owner.authenticatedUser().userId(), demoClient))
                .thenReturn(activation);

        mockMvc.perform(post("/cuenta/simbiosphere/activacion").with(user(owner)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(provisioning).activationLink(owner.authenticatedUser().userId(), demoClient);
    }

    @Test
    void postWithoutCsrfIsRejectedBeforeService() throws Exception {
        PortalUserPrincipal owner = principal("CLIENT");
        mockMvc.perform(post("/cuenta/simbiosphere").with(user(owner)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(provisioning);
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
