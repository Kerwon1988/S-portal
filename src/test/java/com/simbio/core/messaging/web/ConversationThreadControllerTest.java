package com.simbio.core.messaging.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

import com.simbio.core.identity.domain.PermissionEntity;
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
import com.simbio.core.messaging.application.ContactAttachmentService;
import com.simbio.core.messaging.application.StaffReplyService;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationThreadControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-19T14:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationApi conversations;

    @MockitoBean
    private ContactAttachmentService contactAttachments;

    @MockitoBean
    private StaffReplyService replies;

    @Test
    void authorizedEmployeeSeesThreadWithInternalNotesAndEscapedMessageBody() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        ConversationView conversation = conversation();
        when(conversations.findForActor(employee.actor(), conversation.publicId()))
                .thenReturn(Optional.of(conversation));

        var result = mockMvc.perform(get("/gestion/bandeja/{conversationId}", conversation.publicId())
                        .with(user(employee.principal()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("messaging/thread"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Conversation", "Contact", "Internal note", "Restricted staff note")
                .contains("&lt;script&gt;synthetic()&lt;/script&gt;")
                .contains("<main id=\"main-content\"", "<h1", "aria-labelledby=\"thread-messages-heading\"",
                        "<ol", "sm:grid-cols-3", "break-words whitespace-pre-wrap")
                .doesNotContain("<script>synthetic()</script>");
        verify(conversations).findForActor(employee.actor(), conversation.publicId());
    }

    @Test
    void threadUsesSpanishLabelsWhenSpanishLocaleIsRequested() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        ConversationView conversation = conversation();
        when(conversations.findForActor(employee.actor(), conversation.publicId()))
                .thenReturn(Optional.of(conversation));

        String html = mockMvc.perform(get("/gestion/bandeja/{conversationId}", conversation.publicId())
                        .with(user(employee.principal()))
                        .header("Accept-Language", "es"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Conversación", "Contacto", "Nota interna");
    }

    @Test
    void emptyThreadHasAnAccessibleStatusMessage() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.REQUESTS_VIEW_ALL);
        ConversationView conversation = new ConversationView(
                UUID.randomUUID(), ConversationContextType.REQUEST, ConversationStatus.OPEN, NOW, List.of());
        when(conversations.findForActor(employee.actor(), conversation.publicId()))
                .thenReturn(Optional.of(conversation));

        String html = mockMvc.perform(get("/gestion/bandeja/{conversationId}", conversation.publicId())
                        .with(user(employee.principal()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("role=\"status\"", "This conversation has no messages yet.");
    }

    @Test
    void requestViewPermissionCanOpenAuthorizedThread() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.REQUESTS_VIEW_ALL);
        ConversationView conversation = new ConversationView(
                UUID.randomUUID(), ConversationContextType.REQUEST, ConversationStatus.OPEN, NOW, List.of());
        when(conversations.findForActor(employee.actor(), conversation.publicId()))
                .thenReturn(Optional.of(conversation));

        mockMvc.perform(get("/gestion/bandeja/{conversationId}", conversation.publicId())
                        .with(user(employee.principal())))
                .andExpect(status().isOk());

        verify(conversations).findForActor(employee.actor(), conversation.publicId());
    }

    @Test
    void employeesWithoutContextPermissionAndClientsCannotOpenStaffThread() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE);
        TestUser clientWithMisconfiguredInboxPermission = testUser(Role.CLIENT, Permission.INBOX_MANAGE);

        mockMvc.perform(get("/gestion/bandeja/{conversationId}", UUID.randomUUID())
                        .with(user(employee.principal())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/bandeja/{conversationId}", UUID.randomUUID())
                        .with(user(clientWithMisconfiguredInboxPermission.principal())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversations);
    }

    @Test
    void unavailableConversationDoesNotRevealWhetherItExists() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        UUID conversationId = UUID.randomUUID();
        when(conversations.findForActor(employee.actor(), conversationId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/gestion/bandeja/{conversationId}", conversationId)
                        .with(user(employee.principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorizedEmployeeCanPostPublicReplyWithCsrf() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        UUID conversationId = UUID.randomUUID();
        MessageView saved = new MessageView(
                UUID.randomUUID(), MessageAuthorType.EMPLOYEE, MessageVisibility.PUBLIC, "Synthetic reply", NOW);
        when(replies.reply(
                employee.actor(), conversationId, MessageVisibility.PUBLIC, "Synthetic reply", List.of()))
                .thenReturn(saved);

        mockMvc.perform(post("/gestion/bandeja/{conversationId}/responder", conversationId)
                        .with(user(employee.principal()))
                        .with(csrf())
                        .param("body", "Synthetic reply")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestion/bandeja/" + conversationId));

        verify(replies).reply(
                employee.actor(), conversationId, MessageVisibility.PUBLIC, "Synthetic reply", List.of());
    }

    @Test
    void replyRequiresCsrfAndRejectsClientEvenWithInboxPermission() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        TestUser client = testUser(Role.CLIENT, Permission.INBOX_MANAGE);
        UUID conversationId = UUID.randomUUID();

        mockMvc.perform(post("/gestion/bandeja/{conversationId}/responder", conversationId)
                        .with(user(employee.principal()))
                        .param("body", "Synthetic reply")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestion/bandeja/{conversationId}/responder", conversationId)
                        .with(user(client.principal()))
                        .with(csrf())
                        .param("body", "Synthetic reply")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversations);
    }

    @Test
    void invalidReplyIsReturnedWithAccessibleValidationErrorWithoutPosting() throws Exception {
        TestUser employee = testUser(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        UUID conversationId = UUID.randomUUID();
        ConversationView conversation = new ConversationView(
                conversationId, ConversationContextType.CONTACT, ConversationStatus.OPEN, NOW, List.of());
        when(conversations.findForActor(employee.actor(), conversationId)).thenReturn(Optional.of(conversation));

        String html = mockMvc.perform(post("/gestion/bandeja/{conversationId}/responder", conversationId)
                        .with(user(employee.principal()))
                        .with(csrf())
                        .param("body", "")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(view().name("messaging/thread"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Enter a message.", "role=\"alert\"", "aria-invalid=\"true\"");
        verify(conversations).findForActor(employee.actor(), conversationId);
    }

    private static ConversationView conversation() {
        return new ConversationView(
                UUID.randomUUID(),
                ConversationContextType.CONTACT,
                ConversationStatus.OPEN,
                NOW,
                List.of(
                        new MessageView(UUID.randomUUID(), MessageAuthorType.CLIENT, MessageVisibility.PUBLIC,
                                "Synthetic public message", NOW),
                        new MessageView(UUID.randomUUID(), MessageAuthorType.EMPLOYEE, MessageVisibility.INTERNAL,
                                "Restricted staff note <script>synthetic()</script>", NOW.plusSeconds(1))));
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
        return new TestUser(
                new AuthenticatedUser(userId, role, permissionSet),
                new PortalUserPrincipal(account));
    }

    private record TestUser(AuthenticatedUser actor, PortalUserPrincipal principal) {
    }
}
