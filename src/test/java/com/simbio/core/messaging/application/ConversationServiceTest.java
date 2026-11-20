package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessagePosted;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.ClientAccountOwnership;

class ConversationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T14:00:00Z");
    private final ConversationStore store = org.mockito.Mockito.mock(ConversationStore.class);
    private final ClientAccountOwnership customerOwnership =
            org.mockito.Mockito.mock(ClientAccountOwnership.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final ConversationService service = new ConversationService(
            store, customerOwnership, auditRecorder, events, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void beginTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("TC-MSG-002: client query returns only PUBLIC messages and audits access")
    void clientSeesOnlyPublicMessagesForOwnedConversation() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        ConversationEntity conversation = requestConversation(client.userId(), UUID.randomUUID());
        MessageEntity publicMessage = message(conversation, MessageVisibility.PUBLIC, "Synthetic public text");
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);
        when(store.findMessages(conversation.getId(), false)).thenReturn(List.of(publicMessage));

        var result = service.findForActor(client, conversation.getPublicId()).orElseThrow();

        assertThat(result.messages()).extracting("visibility").containsExactly(MessageVisibility.PUBLIC);
        assertThat(result.messages()).extracting("body").containsExactly("Synthetic public text");
        verify(auditRecorder).record(new AuditRecord(client.userId(), "CONVERSATION_READ",
                "CONVERSATION", conversation.getPublicId(), null));
    }

    @Test
    @DisplayName("TC-MSG-002: foreign client cannot enumerate or read a conversation")
    void hidesConversationFromAnotherClient() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        ConversationEntity conversation = requestConversation(UUID.randomUUID(), UUID.randomUUID());
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);

        assertThat(service.findForActor(client, conversation.getPublicId())).isEmpty();

        verify(store, never()).findMessages(anyLong(), anyBoolean());
        verify(auditRecorder, never()).record(any());
    }

    @Test
    @DisplayName("TC-MSG-002: client cannot create an INTERNAL message")
    void deniesClientInternalMessageBeforePersistence() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        ConversationEntity conversation = requestConversation(client.userId(), UUID.randomUUID());
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.postMessage(
                client, conversation.getPublicId(), MessageVisibility.INTERNAL, "Synthetic private text"))
                .isInstanceOf(AccessDeniedException.class);

        verify(store, never()).saveMessage(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("TC-MSG-002: employee needs context-specific permission to access a conversation")
    void deniesEmployeeWithoutInboxPermission() {
        AuthenticatedUser employee = user(Role.EMPLOYEE, Set.of(Permission.REQUESTS_VIEW_ALL));
        ConversationEntity contact = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
        when(store.findConversation(contact.getPublicId())).thenReturn(Optional.of(contact));

        assertThat(service.findForActor(employee, contact.getPublicId())).isEmpty();
        verify(store, never()).findMessages(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("TC-MSG-002: authorized employee can read INTERNAL messages")
    void includesInternalMessagesOnlyForAuthorizedEmployee() {
        AuthenticatedUser employee = user(Role.EMPLOYEE, Set.of(Permission.INBOX_MANAGE));
        ConversationEntity contact = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
        MessageEntity internalMessage = message(contact, MessageVisibility.INTERNAL, "Synthetic staff note");
        when(store.findConversation(contact.getPublicId())).thenReturn(Optional.of(contact));
        when(store.findMessages(contact.getId(), true)).thenReturn(List.of(internalMessage));

        var result = service.findForActor(employee, contact.getPublicId()).orElseThrow();

        assertThat(result.messages()).extracting("visibility").containsExactly(MessageVisibility.INTERNAL);
    }

    @Test
    @DisplayName("TC-MSG-001: request conversation requires an active owning customer account")
    void opensOneRequestConversationForOwningClient() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        UUID requestId = UUID.randomUUID();
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);
        when(store.findByContext(ConversationContextType.REQUEST, requestId)).thenReturn(Optional.empty());
        when(store.saveConversation(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.openRequestConversation(client, requestId);

        assertThat(result.contextType()).isEqualTo(ConversationContextType.REQUEST);
        assertThat(result.status()).isEqualTo(ConversationStatus.OPEN);
        verify(store).saveConversation(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("TC-MSG-001: opening an existing request reuses its conversation")
    void reusesExistingRequestConversation() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        UUID requestId = UUID.randomUUID();
        ConversationEntity existing = requestConversation(client.userId(), requestId);
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);
        when(store.findByContext(ConversationContextType.REQUEST, requestId)).thenReturn(Optional.of(existing));

        var result = service.openRequestConversation(client, requestId);

        assertThat(result.publicId()).isEqualTo(existing.getPublicId());
        verify(store, never()).saveConversation(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("TC-MSG-002: public client message publishes only identifiers after commit")
    void publishesReceivedEventAfterCommitWithoutMessageContent() {
        AuthenticatedUser client = user(Role.CLIENT, Set.of());
        ConversationEntity conversation = requestConversation(client.userId(), UUID.randomUUID());
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(customerOwnership.hasActiveClientAccount(client.userId())).thenReturn(true);
        when(store.saveMessage(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var message = service.postMessage(client, conversation.getPublicId(),
                MessageVisibility.PUBLIC, "Synthetic confidential text");

        assertThat(message.body()).isEqualTo("Synthetic confidential text");
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(synchronization -> synchronization.afterCommit());
        verify(events).publishEvent(new MessageReceived(conversation.getPublicId(), message.publicId()));
    }

    @Test
    @DisplayName("TC-MSG-002: public employee message publishes only identifiers after commit")
    void publishesPostedEventAfterCommit() {
        AuthenticatedUser employee = user(Role.EMPLOYEE, Set.of(Permission.INBOX_MANAGE));
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(store.saveMessage(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var message = service.postMessage(employee, conversation.getPublicId(),
                MessageVisibility.PUBLIC, "Synthetic employee response");

        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(synchronization -> synchronization.afterCommit());
        verify(events).publishEvent(new MessagePosted(conversation.getPublicId(), message.publicId()));
    }

    @Test
    @DisplayName("TC-MSG-002: internal employee notes do not publish notification events")
    void internalEmployeeMessageDoesNotPublishEvent() {
        AuthenticatedUser employee = user(Role.EMPLOYEE, Set.of(Permission.INBOX_MANAGE));
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(store.saveMessage(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.postMessage(employee, conversation.getPublicId(),
                MessageVisibility.INTERNAL, "Synthetic internal note");

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verify(events, never()).publishEvent(any(Object.class));
    }

    private static AuthenticatedUser user(Role role, Set<Permission> permissions) {
        return new AuthenticatedUser(UUID.randomUUID(), role, permissions);
    }

    private static ConversationEntity requestConversation(UUID owner, UUID requestId) {
        return new ConversationEntity(UUID.randomUUID(), ConversationContextType.REQUEST,
                requestId, owner, NOW);
    }

    private static MessageEntity message(
            ConversationEntity conversation,
            MessageVisibility visibility,
            String body) {
        return new MessageEntity(UUID.randomUUID(), conversation, UUID.randomUUID(),
                MessageAuthorType.EMPLOYEE, visibility, body, NOW);
    }
}
