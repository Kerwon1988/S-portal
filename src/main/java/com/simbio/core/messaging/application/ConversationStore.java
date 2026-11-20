package com.simbio.core.messaging.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.messaging.application.InboxConversationSummary;
import com.simbio.core.messaging.application.InboxFilters;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.messaging.domain.MessageEntity;

public interface ConversationStore {

    ConversationEntity saveConversation(ConversationEntity conversation);

    Optional<ConversationEntity> findConversation(UUID publicId);

    Optional<ConversationEntity> findByContext(ConversationContextType contextType, UUID contextPublicId);

    MessageEntity saveMessage(MessageEntity message);

    Optional<MessageEntity> findMessage(UUID publicId);

    List<MessageEntity> findMessages(Long conversationId, boolean includeInternal);

    List<ConversationEntity> findTrashEligibleForPurge(java.time.Instant cutoff, Pageable pageable);

    void deleteConversation(Long conversationId);

    void deleteMessages(Long conversationId);

    ContactSubmissionEntity saveContactSubmission(ContactSubmissionEntity submission);

    Optional<ContactSubmissionEntity> findContactSubmission(Long conversationId);

    MessageAttachmentEntity saveMessageAttachment(MessageAttachmentEntity attachment);

    List<MessageAttachmentEntity> findMessageAttachments(Long conversationId);

    Optional<MessageAttachmentEntity> findMessageAttachment(UUID filePublicId);

    Page<InboxConversationSummary> searchInbox(
            InboxFilters filters,
            UUID actorUserId,
            boolean includeRequests,
            Pageable pageable);
}
