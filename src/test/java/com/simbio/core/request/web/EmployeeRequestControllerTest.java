package com.simbio.core.request.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import com.simbio.core.request.application.EmployeeRequestService;
import com.simbio.core.request.application.QuoteService;
import com.simbio.core.messaging.application.ThreadAccessService;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestSummary;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.RequestDashboard;
import com.simbio.core.request.application.RequestDetail;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeRequestControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean EmployeeRequestService requests;
    @MockitoBean QuoteService quotes;
    @MockitoBean ThreadAccessService threadAccess;
    @MockitoBean DemoProvisioningClient demoProvisioningClient;

    @Test
    void authorizedEmployeeSeesLocalizedDashboard() throws Exception {
        PortalUserPrincipal actor = employee(Permission.REQUESTS_VIEW_ALL);
        when(requests.dashboard(any())).thenReturn(new RequestDashboard(2, 3, 1, 4, -1));

        String html = mockMvc.perform(get("/gestion").with(user(actor)).header("Accept-Language", "en"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Request operations", "Unread tracking remains pending");
    }

    @Test
    void clientCannotReadEmployeeCounts() throws Exception {
        mockMvc.perform(get("/gestion").with(user(client())))
                .andExpect(status().isForbidden());
        verify(requests, never()).dashboard(any());
    }

    @Test
    void quotePostRequiresCsrfAndQuotePermission() throws Exception {
        PortalUserPrincipal actor = employee(Permission.REQUESTS_VIEW_ALL);
        mockMvc.perform(post("/gestion/solicitudes/{id}/presupuesto", UUID.randomUUID())
                        .with(user(actor)))
                .andExpect(status().isForbidden());
        verify(quotes, never()).createVersion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void authorizedEmployeeRendersDetailFragmentsWithCsrf() throws Exception {
        PortalUserPrincipal actor = employee(Permission.REQUESTS_VIEW_ALL, Permission.QUOTES_MANAGE);
        UUID id = UUID.randomUUID();
        RequestDetail detail = new RequestDetail(new RequestSummary(id, "SOL-2026-000001",
                RequestType.CUSTOM_DEVELOPMENT, "WEB_APPLICATION", "Synthetic", RequestStatus.EN_ANALISIS,
                null, null, Instant.parse("2026-11-20T12:00:00Z")), "Synthetic description", null, null,
                null, null, null, List.of(), List.of());
        when(requests.detail(any(), any())).thenReturn(detail);
        when(requests.assignments(any(), any())).thenReturn(List.of());
        when(requests.conversation(any(), any())).thenReturn(new ConversationView(UUID.randomUUID(),
                ConversationContextType.REQUEST, ConversationStatus.OPEN,
                Instant.parse("2026-11-20T12:00:00Z"), List.of()));
        when(quotes.forEmployee(any(), any())).thenReturn(List.of());

        String html = mockMvc.perform(get("/gestion/solicitudes/{id}", id).with(user(actor))
                        .with(csrf()).header("Accept-Language", "es"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Synthetic description", "Presupuesto", "_csrf");
    }

    @Test
    void authorizedEmployeeCanSeeInternalNoteButClientCannotOpenEmployeeDetail() throws Exception {
        PortalUserPrincipal actor = employee(Permission.REQUESTS_VIEW_ALL);
        UUID id = UUID.randomUUID();
        RequestDetail detail = new RequestDetail(new RequestSummary(id, "SOL-2026-000002",
                RequestType.CUSTOM_DEVELOPMENT, "WEB_APPLICATION", "Synthetic", RequestStatus.EN_ANALISIS,
                null, null, Instant.parse("2026-11-20T12:00:00Z")), "Synthetic description", null, null,
                null, null, null, List.of(), List.of());
        ConversationView conversation = new ConversationView(UUID.randomUUID(), ConversationContextType.REQUEST,
                ConversationStatus.OPEN, Instant.parse("2026-11-20T12:00:00Z"), List.of(
                        new MessageView(UUID.randomUUID(), MessageAuthorType.EMPLOYEE,
                                MessageVisibility.INTERNAL, "Synthetic restricted staff note",
                                Instant.parse("2026-11-20T12:00:00Z"))));
        when(requests.detail(any(), any())).thenReturn(detail);
        when(requests.assignments(any(), any())).thenReturn(List.of());
        when(requests.conversation(any(), any())).thenReturn(conversation);
        when(quotes.forEmployee(any(), any())).thenReturn(List.of());

        String html = mockMvc.perform(get("/gestion/solicitudes/{id}", id).with(user(actor)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Synthetic restricted staff note");
        mockMvc.perform(get("/gestion/solicitudes/{id}", id).with(user(client())))
                .andExpect(status().isForbidden());
    }

    private static PortalUserPrincipal employee(Permission... permissions) {
        return principal("EMPLOYEE", permissions);
    }

    private static PortalUserPrincipal client() { return principal("CLIENT"); }

    private static PortalUserPrincipal principal(String roleCode, Permission... permissions) {
        RoleEntity role = new RoleEntity(roleCode, roleCode);
        for (Permission permission : permissions) {
            role.getPermissions().add(new PermissionEntity(permission.name(), permission.name()));
        }
        UserAccountEntity account = new UserAccountEntity(UUID.randomUUID(), roleCode.toLowerCase() + "@example.test",
                "synthetic-password-hash", "en", Instant.parse("2026-11-20T12:00:00Z"), role);
        return new PortalUserPrincipal(account);
    }
}
