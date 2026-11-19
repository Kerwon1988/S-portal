package com.simbio.core.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessagePosted;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.ClientAccountOwnership;

@Service
public class ConversationService implements ConversationApi {

    private final ConversationStore store;
    private final ClientAccountOwnership customerOwnership;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public ConversationService(
            ConversationStore store,
            ClientAccountOwnership customerOwnership,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events) {
        this(store, customerOwnership, auditRecorder, events, Clock.systemUTC());
    }

    ConversationService(
            ConversationStore store,
            ClientAccountOwnership customerOwnership,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events,
            Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.customerOwnership = Objects.requireNonNull(customerOwnership);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public ConversationView openRequestConversation(AuthenticatedUser actor, UUID requestPublicId) {
        requireActiveCustomer(actor);
        if (requestPublicId == null) {
            throw new IllegalArgumentException("Request reference is required");
        }
        ConversationEntity conversation = store.findByContext(
                        ConversationContextType.REQUEST, requestPublicId)
                .orElseGet(() -> store.saveConversation(new ConversationEntity(
                        UUID.randomUUID(), ConversationContextType.REQUEST, requestPublicId,
                        actor.userId(), Instant.now(clock))));
        if (!actor.userId().equals(conversation.getOwnerUserId())) {
            throw new ConversationNotFoundException();
        }
        return view(conversation, List.of());
    }

    @Override
    @Transactional
    public Optional<ConversationView> findForActor(AuthenticatedUser actor, UUID conversationPublicId) {
        if (actor == null || conversationPublicId == null) {
            throw new IllegalArgumentException("Actor and conversation reference are required");
        }
        Optional<ConversationEntity> found = store.findConversation(conversationPublicId);
        if (found.isEmpty() || !canRead(actor, found.get())) {
            return Optional.empty();
        }
        ConversationEntity conversation = found.get();
        List<MessageView> messages = store.findMessages(
                        conversation.getId(), actor.role() != Role.CLIENT).stream()
                .map(ConversationService::messageView)
                .toList();
        auditRecorder.record(new AuditRecord(actor.userId(), "CONVERSATION_READ",
                "CONVERSATION", conversation.getPublicId(), null));
        return Optional.of(view(conversation, messages));
    }

    @Override
    @Transactional
    public MessageView postMessage(
            AuthenticatedUser actor,
            UUID conversationPublicId,
            MessageVisibility visibility,
            String body) {
        if (actor == null || conversationPublicId == null || visibility == null) {
            throw new IllegalArgumentException("Actor, conversation and visibility are required");
        }
        ConversationEntity conversation = store.findConversation(conversationPublicId)
                .filter(value -> canRead(actor, value))
                .orElseThrow(ConversationNotFoundException::new);
        if (conversation.getStatus() != ConversationStatus.OPEN) {
            throw new ConversationClosedException();
        }
        if (actor.role() == Role.CLIENT && visibility != MessageVisibility.PUBLIC) {
            throw new AccessDeniedException("Clients cannot create internal messages");
        }
        MessageAuthorType authorType = authorType(actor.role());
        MessageEntity message = store.saveMessage(new MessageEntity(
                UUID.randomUUID(), conversation, actor.userId(), authorType,
                visibility, body, Instant.now(clock)));
        auditRecorder.record(new AuditRecord(actor.userId(), "MESSAGE_POSTED",
                "MESSAGE", message.getPublicId(), null));
        if (visibility == MessageVisibility.PUBLIC) {
            Object event = actor.role() == Role.CLIENT
                    ? new MessageReceived(conversation.getPublicId(), message.getPublicId())
                    : new MessagePosted(conversation.getPublicId(), message.getPublicId());
            publishAfterCommit(event);
        }
        return messageView(message);
    }

    private boolean canRead(AuthenticatedUser actor, ConversationEntity conversation) {
        if (actor.role() == Role.CLIENT) {
            return actor.userId().equals(conversation.getOwnerUserId())
                    && hasActiveCustomerAccount(actor.userId());
        }
        if (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN) {
            return false;
        }
        Permission permission = conversation.getContextType() == ConversationContextType.CONTACT
                ? Permission.INBOX_MANAGE : Permission.REQUESTS_VIEW_ALL;
        return actor.hasPermission(permission);
    }

    private void requireActiveCustomer(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.CLIENT || !hasActiveCustomerAccount(actor.userId())) {
            throw new AccessDeniedException("Active client account required");
        }
    }

    private boolean hasActiveCustomerAccount(UUID userId) {
        return customerOwnership.hasActiveClientAccount(userId);
    }

    private static MessageAuthorType authorType(Role role) {
        return switch (role) {
            case CLIENT -> MessageAuthorType.CLIENT;
            case EMPLOYEE -> MessageAuthorType.EMPLOYEE;
            case ADMIN -> MessageAuthorType.ADMIN;
        };
    }

    private static ConversationView view(ConversationEntity conversation, List<MessageView> messages) {
        return new ConversationView(conversation.getPublicId(), conversation.getContextType(),
                conversation.getStatus(), conversation.getCreatedAt(), messages);
    }

    private static MessageView messageView(MessageEntity message) {
        return new MessageView(message.getPublicId(), message.getAuthorType(), message.getVisibility(),
                message.getBody(), message.getCreatedAt());
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Message events require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }
}
