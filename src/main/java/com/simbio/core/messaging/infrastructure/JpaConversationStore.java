package com.simbio.core.messaging.infrastructure;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.simbio.core.messaging.application.ConversationStore;
import com.simbio.core.messaging.application.InboxConversationSummary;
import com.simbio.core.messaging.application.InboxFilters;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.api.ConversationInboxState;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.messaging.domain.MessageEntity;

@Repository
class JpaConversationStore implements ConversationStore {

    private final ConversationJpaRepository conversations;
    private final MessageJpaRepository messages;
    private final ContactSubmissionJpaRepository contactSubmissions;
    private final MessageAttachmentJpaRepository attachments;

    JpaConversationStore(
            ConversationJpaRepository conversations,
            MessageJpaRepository messages,
            ContactSubmissionJpaRepository contactSubmissions,
            MessageAttachmentJpaRepository attachments) {
        this.conversations = conversations;
        this.messages = messages;
        this.contactSubmissions = contactSubmissions;
        this.attachments = attachments;
    }

    @Override
    public ConversationEntity saveConversation(ConversationEntity conversation) {
        return conversations.save(conversation);
    }

    @Override
    public Optional<ConversationEntity> findConversation(UUID publicId) {
        return conversations.findByPublicId(publicId);
    }

    @Override
    public Optional<ConversationEntity> findByContext(
            ConversationContextType contextType,
            UUID contextPublicId) {
        return conversations.findByContextTypeAndContextPublicId(contextType, contextPublicId);
    }

    @Override
    public MessageEntity saveMessage(MessageEntity message) {
        return messages.save(message);
    }

    @Override
    public Optional<MessageEntity> findMessage(UUID publicId) {
        return messages.findByPublicId(publicId);
    }

    @Override
    public List<MessageEntity> findMessages(Long conversationId, boolean includeInternal) {
        return messages.findForConversation(conversationId, includeInternal);
    }

    @Override
    public List<ConversationEntity> findTrashEligibleForPurge(
            java.time.Instant cutoff, Pageable pageable) {
        return conversations.findByInboxStateAndStateChangedAtLessThanEqualOrderByStateChangedAtAsc(
                ConversationInboxState.TRASH, cutoff, pageable);
    }

    @Override
    public void deleteConversation(Long conversationId) {
        conversations.deleteById(conversationId);
    }

    @Override
    public void deleteMessages(Long conversationId) {
        messages.deleteByConversationId(conversationId);
    }

    @Override
    public ContactSubmissionEntity saveContactSubmission(ContactSubmissionEntity submission) {
        return contactSubmissions.save(submission);
    }

    @Override
    public Optional<ContactSubmissionEntity> findContactSubmission(Long conversationId) {
        return contactSubmissions.findByConversationId(conversationId);
    }

    @Override
    public MessageAttachmentEntity saveMessageAttachment(MessageAttachmentEntity attachment) {
        return attachments.save(attachment);
    }

    @Override
    public List<MessageAttachmentEntity> findMessageAttachments(Long conversationId) {
        return attachments.findByConversationId(conversationId);
    }

    @Override
    public Optional<MessageAttachmentEntity> findMessageAttachment(UUID filePublicId) {
        return attachments.findByFilePublicId(filePublicId);
    }

    @Override
    public Page<InboxConversationSummary> searchInbox(
            InboxFilters filters,
            UUID actorUserId,
            boolean includeRequests,
            Pageable pageable) {
        return conversations.searchInbox(
                filters.inboxState(),
                filters.contextType(),
                filters.status(),
                filters.assignedToActorOnly(),
                filters.unassignedOnly(),
                actorUserId,
                includeRequests,
                ConversationContextType.CONTACT,
                filters.unreadOnly(),
                filters.searchTerm().toLowerCase(Locale.ROOT),
                MessageVisibility.PUBLIC,
                List.of(MessageAuthorType.ANONYMOUS, MessageAuthorType.CLIENT),
                pageable);
    }
}
