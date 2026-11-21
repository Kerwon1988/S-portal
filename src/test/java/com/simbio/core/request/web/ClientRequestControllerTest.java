package com.simbio.core.request.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.application.ThreadAccessService;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestSummary;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.QuoteService;
import com.simbio.core.request.application.RequestDetail;
import com.simbio.core.request.application.RequestService;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;
import com.simbio.core.simbiosphere.api.LinkedBusinessQuery;
import com.simbio.core.shared.paging.PageResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientRequestControllerTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    @Autowired MockMvc mockMvc;
    @MockitoBean RequestService requests;
    @MockitoBean QuoteService quotes;
    @MockitoBean ConversationApi conversations;
    @MockitoBean ThreadAccessService threadAccess;
    @MockitoBean DemoProvisioningClient demoProvisioningClient;
    @MockitoBean LinkedBusinessQuery linkedBusinesses;

    @Test
    void listRendersLocalizedFiltersAndPagination() throws Exception {
        TestUser client = client();
        var summary = detail(UUID.randomUUID()).summary();
        when(requests.listOwned(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(summary), 0, 10, 11));

        String html = mockMvc.perform(get("/cuenta/solicitudes")
                        .with(user(client.principal())).header("Accept-Language", "en")
                        .param("reference", "SOL-2026"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("My requests", "From", "To", "SOL-2026-000001", "page=1")
                .contains("reference=SOL-2026");
    }

    @Test
    void clientDetailNeverRendersInternalMessages() throws Exception {
        TestUser client = client();
        UUID requestId = UUID.randomUUID();
        RequestDetail detail = detail(requestId);
        when(requests.ownedDetail(client.actor(), requestId)).thenReturn(detail);
        when(conversations.findRequestForActor(client.actor(), requestId)).thenReturn(Optional.of(
                new ConversationView(UUID.randomUUID(), ConversationContextType.REQUEST,
                        ConversationStatus.OPEN, NOW, List.of(
                                message(MessageVisibility.PUBLIC, "Synthetic public update"),
                                message(MessageVisibility.INTERNAL, "Synthetic restricted staff note")))));

        String html = mockMvc.perform(get("/cuenta/solicitudes/{id}", requestId)
                        .with(user(client.principal())).header("Accept-Language", "en"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Synthetic public update").doesNotContain("Synthetic restricted staff note");
    }

    @Test
    void foreignRequestIsPresentedAsNotFound() throws Exception {
        TestUser client = client();
        UUID foreignId = UUID.randomUUID();
        when(requests.ownedDetail(client.actor(), foreignId)).thenThrow(new RequestNotFoundException());

        mockMvc.perform(get("/cuenta/solicitudes/{id}", foreignId).with(user(client.principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientMutationRequiresCsrf() throws Exception {
        TestUser client = client();
        mockMvc.perform(post("/cuenta/solicitudes/{id}/cancelar", UUID.randomUUID())
                        .with(user(client.principal())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(requests);
    }

    @Test
    void employeeCannotOpenClientCreationForm() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE);
        mockMvc.perform(get("/cuenta/solicitudes/nueva").with(user(employee.principal())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(requests);
    }

    private static RequestDetail detail(UUID requestId) {
        return new RequestDetail(new RequestSummary(requestId, "SOL-2026-000001",
                RequestType.CUSTOM_DEVELOPMENT, "WEB_APPLICATION", "Synthetic title",
                RequestStatus.RECIBIDA, null, null, NOW),
                "Synthetic description", null, null, null, null, null, List.of(), List.of());
    }

    private static MessageView message(MessageVisibility visibility, String body) {
        return new MessageView(UUID.randomUUID(), MessageAuthorType.EMPLOYEE, visibility, body, NOW);
    }

    private static TestUser client() { return testUser(Role.CLIENT); }

    private static TestUser testUser(Role role) {
        UUID userId = UUID.randomUUID();
        RoleEntity roleEntity = new RoleEntity(role.name(), role.name());
        UserAccountEntity account = new UserAccountEntity(userId, role.name().toLowerCase() + "@example.test",
                "synthetic-password-hash", "en", NOW, roleEntity);
        return new TestUser(new AuthenticatedUser(userId, role, Set.of()), new PortalUserPrincipal(account));
    }

    private record TestUser(AuthenticatedUser actor, PortalUserPrincipal principal) { }
}
