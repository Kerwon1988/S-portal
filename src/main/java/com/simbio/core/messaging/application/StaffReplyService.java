package com.simbio.core.messaging.application;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.config.MessagingContactProperties;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.shared.security.AuthenticatedUser;

@Service
public class StaffReplyService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp");

    private final ConversationApi conversations;
    private final ConversationStore store;
    private final FileStorage files;
    private final MessagingContactProperties properties;
    private final Tika tika = new Tika();
    private final Clock clock;

    @Autowired
    public StaffReplyService(
            ConversationApi conversations,
            ConversationStore store,
            FileStorage files,
            MessagingContactProperties properties) {
        this(conversations, store, files, properties, Clock.systemUTC());
    }

    StaffReplyService(
            ConversationApi conversations,
            ConversationStore store,
            FileStorage files,
            MessagingContactProperties properties,
            Clock clock) {
        this.conversations = Objects.requireNonNull(conversations);
        this.store = Objects.requireNonNull(store);
        this.files = Objects.requireNonNull(files);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public MessageView reply(
            AuthenticatedUser actor,
            UUID conversationPublicId,
            MessageVisibility visibility,
            String body,
            List<ContactAttachmentInput> attachments) {
        MessageView message = conversations.postMessage(actor, conversationPublicId, visibility, body);
        List<ContactAttachmentInput> safeAttachments = attachments == null ? List.of() : attachments;
        if (safeAttachments.size() > properties.maxAttachments()) {
            throw new ContactSubmissionRejectedException();
        }
        long total = 0;
        for (ContactAttachmentInput attachment : safeAttachments) {
            validate(attachment);
            total += attachment.content().length;
            if (total > properties.maxTotalAttachmentBytes()) {
                throw new ContactSubmissionRejectedException();
            }
            String detectedMime = tika.detect(attachment.content(), attachment.submittedName());
            if (!ALLOWED_MIME.contains(detectedMime)) {
                throw new ContactSubmissionRejectedException();
            }
            StoredFile stored = files.store(new ByteArrayInputStream(attachment.content()),
                    attachment.submittedName(), attachment.content().length, FilePurpose.ATTACHMENT);
            if (stored.scanState() != ScanState.CLEAN || !detectedMime.equals(stored.detectedMime())) {
                throw new ContactSubmissionRejectedException();
            }
            store.findMessage(message.publicId()).ifPresentOrElse(
                    entity -> store.saveMessageAttachment(new MessageAttachmentEntity(
                            entity.getId(), stored.publicId(), stored.detectedMime(), stored.sizeBytes(),
                            Instant.now(clock))),
                    () -> { throw new ConversationNotFoundException(); });
        }
        return message;
    }

    private void validate(ContactAttachmentInput input) {
        if (input == null || input.content() == null || input.content().length == 0
                || input.declaredSize() != input.content().length
                || input.content().length > properties.maxAttachmentBytes()
                || input.submittedName() == null || input.submittedName().isBlank()
                || input.submittedName().length() > 255
                || input.submittedName().contains("/") || input.submittedName().contains("\\")
                || input.submittedName().contains("..")) {
            throw new ContactSubmissionRejectedException();
        }
    }
}
