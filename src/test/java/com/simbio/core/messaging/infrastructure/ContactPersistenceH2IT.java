package com.simbio.core.messaging.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.messaging.application.ConversationStore;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.messaging.domain.MessageEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConversationStore.class)
class ContactPersistenceH2IT {

    @Autowired private ConversationStore store;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void persistsContactConsentMetadataAndCleanAttachmentReferenceTogether() {
        Instant now = Instant.parse("2026-11-20T13:00:00Z");
        ConversationEntity conversation = store.saveConversation(new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, now));
        MessageEntity message = store.saveMessage(new MessageEntity(UUID.randomUUID(), conversation, null,
                MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, "Synthetic public message", now));
        store.saveContactSubmission(new ContactSubmissionEntity(UUID.randomUUID(), conversation.getId(),
                "encrypted-name", "encrypted-email", "encrypted-phone", "Synthetic reason", "es", 3,
                "a".repeat(64), now));
        UUID fileId = UUID.randomUUID();
        store.saveMessageAttachment(new MessageAttachmentEntity(
                message.getId(), fileId, "application/pdf", 42, now));
        entityManager.flush();
        entityManager.clear();

        ConversationEntity reloaded = store.findConversation(conversation.getPublicId()).orElseThrow();
        assertThat(store.findContactSubmission(reloaded.getId())).hasValueSatisfying(value -> {
            assertThat(value.getConsentVersion()).isEqualTo(3);
            assertThat(value.getEmailEncrypted()).isEqualTo("encrypted-email");
            assertThat(value.toString()).doesNotContain("encrypted-email");
        });
        assertThat(store.findMessageAttachments(reloaded.getId()))
                .singleElement().satisfies(value -> assertThat(value.getFilePublicId()).isEqualTo(fileId));
    }
}
