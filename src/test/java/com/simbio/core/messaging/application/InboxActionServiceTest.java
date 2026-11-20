package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.config.MessagingRetentionProperties;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.api.ConversationInboxState;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class InboxActionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T17:00:00Z");

    private ConversationStore store;
    private AuditRecorder audit;
    private ConversationEntity conversation;
    private InboxActionService service;

    @BeforeEach
    void setUp() {
        store = mock(ConversationStore.class);
        audit = mock(AuditRecorder.class);
        conversation = new ConversationEntity(UUID.randomUUID(), ConversationContextType.CONTACT,
                UUID.randomUUID(), null, NOW.minusSeconds(60));
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        service = new InboxActionService(store, audit,
                new MessagingRetentionProperties(Duration.ofDays(30)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void authorizedEmployeeCanAssignLabelAndMoveConversationThroughOperationalStates() {
        var actor = employee(Permission.INBOX_MANAGE);
        UUID assignee = UUID.randomUUID();

        service.assign(actor, conversation.getPublicId(), assignee);
        service.updateLabels(actor, conversation.getPublicId(), "facturacion, urgente");
        service.archive(actor, conversation.getPublicId());
        service.moveToTrash(actor, conversation.getPublicId());
        service.restore(actor, conversation.getPublicId());

        assertThat(conversation.getAssignedTo()).isEqualTo(assignee);
        assertThat(conversation.getLabels()).isEqualTo("facturacion, urgente");
        assertThat(conversation.getInboxState()).isEqualTo(ConversationInboxState.ACTIVE);
    }

    @Test
    void clientCannotPerformInboxActions() {
        AuthenticatedUser client = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());

        assertThatThrownBy(() -> service.archive(client, conversation.getPublicId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void employeeCannotPurgeEvenWhenConversationIsEligible() {
        conversation.moveToTrash(NOW.minus(Duration.ofDays(31)));

        assertThatThrownBy(() -> service.purge(employee(Permission.INBOX_MANAGE, Permission.INBOX_PURGE),
                conversation.getPublicId())).isInstanceOf(AccessDeniedException.class);
        verify(store, never()).deleteMessages(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adminCannotPurgeWhileLegalRetentionIsDisabled() {
        service = new InboxActionService(store, audit,
                new MessagingRetentionProperties(Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));
        conversation.moveToTrash(NOW.minus(Duration.ofDays(31)));

        assertThatThrownBy(() -> service.purge(admin(Permission.INBOX_PURGE), conversation.getPublicId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adminPurgesEligibleTrashAfterGracePeriod() {
        conversation.moveToTrash(NOW.minus(Duration.ofDays(31)));

        service.purge(admin(Permission.INBOX_PURGE), conversation.getPublicId());

        verify(store).deleteMessages(conversation.getId());
        verify(store).deleteConversation(conversation.getId());
    }

    @Test
    void readAndUnreadActionsOnlyUpdateStoredMessages() {
        MessageEntity message = mock(MessageEntity.class);
        when(store.findMessages(conversation.getId(), true)).thenReturn(List.of(message));
        when(message.getReadAt()).thenReturn(null, NOW);

        service.markRead(employee(Permission.INBOX_MANAGE), conversation.getPublicId());
        service.markUnread(employee(Permission.INBOX_MANAGE), conversation.getPublicId());

        verify(message).markRead(NOW);
        verify(message).markUnread();
    }

    private static AuthenticatedUser employee(Permission... permissions) {
        return new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of(permissions));
    }

    private static AuthenticatedUser admin(Permission... permissions) {
        return new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN, Set.of(permissions));
    }
}
