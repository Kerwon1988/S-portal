package com.simbio.core.messaging.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
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
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.application.InboxAssignmentFilter;
import com.simbio.core.messaging.application.InboxConversationSummary;
import com.simbio.core.messaging.application.InboxFilters;
import com.simbio.core.messaging.application.InboxService;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InboxControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-19T14:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inbox;

    @Test
    void authorizedEmployeeGetsLocalizedInboxWithoutRequestOptionWhenNotPermitted() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        when(inbox.search(eq(employee.actor()), any(), any())).thenReturn(pageWithConversation());

        String html = mockMvc.perform(get("/gestion/bandeja")
                        .with(user(employee.principal()))
                        .header("Accept-Language", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("messaging/inbox"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Bandeja de conversaciones", "Buscar en mensajes públicos",
                        "Solo conversaciones no leídas", "hx-post=\"/gestion/bandeja/buscar\"",
                        "aria-live=\"polite\"", "for=\"inbox-search\"", "<caption",
                        "scope=\"col\"", "sm:hidden", "focus-visible:outline-2", "min-h-11")
                .doesNotContain("value=\"REQUEST\"");
        verify(inbox).search(eq(employee.actor()),
                eq(new InboxFilters(null, null, InboxAssignmentFilter.ALL, false, "")),
                eq(PageQuery.of(0, 20)));
    }

    @Test
    void postHtmxSearchUsesRequestBodyAndReturnsOnlyResultsFragment() throws Exception {
        TestUser employee = testUser(
                Role.EMPLOYEE, Permission.INBOX_MANAGE, Permission.REQUESTS_VIEW_ALL);
        when(inbox.search(eq(employee.actor()), any(), any())).thenReturn(emptyPage());

        String fragment = mockMvc.perform(post("/gestion/bandeja/buscar")
                        .with(user(employee.principal()))
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("contextType", "CONTACT")
                        .param("assignment", "MINE")
                        .param("unreadOnly", "true")
                        .param("searchTerm", "Synthetic public phrase")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("messaging/inbox :: results"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(fragment).contains("<section id=\"inbox-results\"")
                .doesNotContain("<main id=\"main-content\"");
        verify(inbox).search(eq(employee.actor()),
                eq(new InboxFilters(ConversationContextType.CONTACT, null,
                        InboxAssignmentFilter.MINE, true, "Synthetic public phrase")),
                eq(PageQuery.of(1, 20)));
    }

    @Test
    void rejectsClientAndMissingCsrfBeforeCallingInboxService() throws Exception {
        TestUser client = testUser(Role.CLIENT, Permission.INBOX_MANAGE);
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);

        mockMvc.perform(get("/gestion/bandeja").with(user(client.principal())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestion/bandeja/buscar")
                        .with(user(employee.principal()))
                        .param("searchTerm", "Synthetic phrase"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(inbox);
    }

    @Test
    void rejectsOverlongSearchTermWithoutEchoingIt() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        String longTerm = "sensitive".repeat(20);

        String response = mockMvc.perform(post("/gestion/bandeja/buscar")
                        .with(user(employee.principal()))
                        .with(csrf())
                        .param("searchTerm", longTerm))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain(longTerm);
        verifyNoInteractions(inbox);
    }

    private static PageResult<InboxConversationSummary> emptyPage() {
        return new PageResult<>(java.util.List.of(), 0, 20, 0);
    }

    private static PageResult<InboxConversationSummary> pageWithConversation() {
        InboxConversationSummary summary = new InboxConversationSummary(
                UUID.randomUUID(), ConversationContextType.CONTACT, ConversationStatus.OPEN, null, NOW);
        return new PageResult<>(java.util.List.of(summary), 0, 20, 1);
    }

    private static TestUser testUser(Role role, Permission... permissions) {
        UUID userId = UUID.randomUUID();
        RoleEntity roleEntity = new RoleEntity(role.name(), role.name());
        Set<Permission> permissionSet = Set.of(permissions);
        permissionSet.forEach(permission -> roleEntity.getPermissions().add(
                new PermissionEntity(permission.name(), permission.name())));
        UserAccountEntity account = new UserAccountEntity(
                userId,
                role.name().toLowerCase() + "@example.test",
                "synthetic-password-hash",
                "en",
                NOW,
                roleEntity);
        return new TestUser(new AuthenticatedUser(userId, role, permissionSet), new PortalUserPrincipal(account));
    }

    private record TestUser(AuthenticatedUser actor, PortalUserPrincipal principal) {
    }
}
