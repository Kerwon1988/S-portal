package com.simbio.core.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.config.MessagingRetentionProperties;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.api.ConversationInboxState;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class InboxActionService {

    private final ConversationStore store;
    private final AuditRecorder audit;
    private final MessagingRetentionProperties retention;
    private final Clock clock;

    @Autowired
    public InboxActionService(
            ConversationStore store,
            AuditRecorder audit,
            MessagingRetentionProperties retention) {
        this(store, audit, retention, Clock.systemUTC());
    }

    InboxActionService(
            ConversationStore store,
            AuditRecorder audit,
            MessagingRetentionProperties retention,
            Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.audit = Objects.requireNonNull(audit);
        this.retention = Objects.requireNonNull(retention);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public void assign(AuthenticatedUser actor, UUID conversationId, UUID assigneeId) {
        ConversationEntity conversation = managed(actor, conversationId);
        if (assigneeId == null) {
            conversation.unassign();
        } else {
            conversation.assignTo(assigneeId);
        }
        audit(actor, conversation, "CONVERSATION_ASSIGNED", value(assigneeId));
    }

    @Transactional
    public void updateLabels(AuthenticatedUser actor, UUID conversationId, String labels) {
        ConversationEntity conversation = managed(actor, conversationId);
        conversation.updateLabels(labels);
        audit(actor, conversation, "CONVERSATION_LABELS_UPDATED", conversation.getLabels());
    }

    @Transactional
    public void markRead(AuthenticatedUser actor, UUID conversationId) {
        ConversationEntity conversation = managed(actor, conversationId);
        Instant now = Instant.now(clock);
        store.findMessages(conversation.getId(), true).stream()
                .filter(message -> message.getReadAt() == null)
                .forEach(message -> message.markRead(now));
        audit(actor, conversation, "CONVERSATION_READ", null);
    }

    @Transactional
    public void markUnread(AuthenticatedUser actor, UUID conversationId) {
        ConversationEntity conversation = managed(actor, conversationId);
        store.findMessages(conversation.getId(), true).stream()
                .filter(message -> message.getReadAt() != null)
                .forEach(message -> message.markUnread());
        audit(actor, conversation, "CONVERSATION_UNREAD", null);
    }

    @Transactional
    public void archive(AuthenticatedUser actor, UUID conversationId) {
        ConversationEntity conversation = managed(actor, conversationId);
        conversation.archive(Instant.now(clock));
        audit(actor, conversation, "CONVERSATION_ARCHIVED", null);
    }

    @Transactional
    public void moveToTrash(AuthenticatedUser actor, UUID conversationId) {
        ConversationEntity conversation = managed(actor, conversationId);
        conversation.moveToTrash(Instant.now(clock));
        audit(actor, conversation, "CONVERSATION_TRASHED", null);
    }

    @Transactional
    public void restore(AuthenticatedUser actor, UUID conversationId) {
        ConversationEntity conversation = managed(actor, conversationId);
        conversation.restore(Instant.now(clock));
        audit(actor, conversation, "CONVERSATION_RESTORED", null);
    }

    @Transactional
    public void purge(AuthenticatedUser actor, UUID conversationId) {
        requirePurgePermission(actor);
        ConversationEntity conversation = store.findConversation(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        Instant eligibleAt = conversation.getStateChangedAt() == null
                ? null : conversation.getStateChangedAt().plus(retention.trashGracePeriod());
        if (!retention.purgeEnabled()
                || conversation.getInboxState() != ConversationInboxState.TRASH
                || eligibleAt == null
                || eligibleAt.isAfter(Instant.now(clock))) {
            throw new IllegalStateException("Conversation is not eligible for permanent purge");
        }
        audit(actor, conversation, "CONVERSATION_PURGED", null);
        store.deleteMessages(conversation.getId());
        store.deleteConversation(conversation.getId());
    }

    private ConversationEntity managed(AuthenticatedUser actor, UUID conversationId) {
        requireInboxPermission(actor);
        ConversationEntity conversation = store.findConversation(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        return conversation;
    }

    private void audit(AuthenticatedUser actor, ConversationEntity conversation, String action, String details) {
        audit.record(new AuditRecord(actor.userId(), action, "CONVERSATION",
                conversation.getPublicId(), details));
    }

    private static String value(UUID value) {
        return value == null ? null : value.toString();
    }

    private static void requireInboxPermission(AuthenticatedUser actor) {
        if (actor == null
                || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)
                || !actor.hasPermission(Permission.INBOX_MANAGE)) {
            throw new AccessDeniedException("Inbox management permission required");
        }
    }

    private static void requirePurgePermission(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.INBOX_PURGE)) {
            throw new AccessDeniedException("Inbox purge permission required");
        }
    }
}
