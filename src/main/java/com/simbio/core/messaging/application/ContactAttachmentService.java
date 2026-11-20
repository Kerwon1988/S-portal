package com.simbio.core.messaging.application;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class ContactAttachmentService {

    private final ConversationStore store;
    private final FileStorage files;
    private final ConversationApi conversations;

    @org.springframework.beans.factory.annotation.Autowired
    public ContactAttachmentService(ConversationStore store, FileStorage files, ConversationApi conversations) {
        this.store = store;
        this.files = files;
        this.conversations = conversations;
    }

    ContactAttachmentService(ConversationStore store, FileStorage files) {
        this.store = store;
        this.files = files;
        this.conversations = null;
    }

    @Transactional(readOnly = true)
    public List<ContactAttachmentView> list(AuthenticatedUser actor, UUID conversationPublicId) {
        ConversationEntity conversation = authorizedContact(actor, conversationPublicId);
        return store.findMessageAttachments(conversation.getId()).stream()
                .map(value -> new ContactAttachmentView(value.getFilePublicId(),
                        value.getDetectedMime(), value.getSizeBytes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactAttachmentDownload download(AuthenticatedUser actor, UUID conversationPublicId,
            UUID filePublicId) {
        ConversationEntity conversation = authorizedContact(actor, conversationPublicId);
        MessageAttachmentEntity attachment = store.findMessageAttachment(filePublicId)
                .filter(value -> store.findMessageAttachments(conversation.getId()).stream()
                        .anyMatch(candidate -> candidate.getFilePublicId().equals(filePublicId)))
                .orElseThrow(ContactAttachmentNotFoundException::new);
        InputStream content = files.open(filePublicId,
                requested -> requested.equals(attachment.getFilePublicId()));
        return new ContactAttachmentDownload(attachment.getDetectedMime(), attachment.getSizeBytes(), content);
    }

    private ConversationEntity authorizedContact(AuthenticatedUser actor, UUID conversationPublicId) {
        if (conversations != null) {
            conversations.findForActor(actor, conversationPublicId)
                    .orElseThrow(ContactAttachmentNotFoundException::new);
            return store.findConversation(conversationPublicId)
                    .orElseThrow(ContactAttachmentNotFoundException::new);
        }
        requireManager(actor);
        return store.findConversation(conversationPublicId)
                .filter(value -> value.getContextType() == ConversationContextType.CONTACT)
                .orElseThrow(ContactAttachmentNotFoundException::new);
    }

    private static void requireManager(AuthenticatedUser actor) {
        if (actor == null || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)
                || !actor.hasPermission(Permission.INBOX_MANAGE)) {
            throw new AccessDeniedException("Inbox permission required");
        }
    }
}
