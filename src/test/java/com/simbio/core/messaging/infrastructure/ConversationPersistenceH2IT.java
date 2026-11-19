package com.simbio.core.messaging.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.messaging.application.ConversationStore;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConversationStore.class)
class ConversationPersistenceH2IT {

    private static final Instant NOW = Instant.parse("2026-11-19T14:00:00Z");

    @Autowired
    private ConversationStore store;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("TC-MSG-001: H2 persists CONTACT and REQUEST context with ordered messages")
    void persistsConversationContextAndOrderedMessages() {
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.REQUEST, UUID.randomUUID(), UUID.randomUUID(), NOW);
        store.saveConversation(conversation);
        store.saveMessage(new MessageEntity(UUID.randomUUID(), conversation, UUID.randomUUID(),
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, "Synthetic first message", NOW));
        store.saveMessage(new MessageEntity(UUID.randomUUID(), conversation, UUID.randomUUID(),
                MessageAuthorType.EMPLOYEE, MessageVisibility.INTERNAL, "Synthetic internal note",
                NOW.plusSeconds(1)));
        entityManager.flush();
        entityManager.clear();

        ConversationEntity reloaded = store.findConversation(conversation.getPublicId()).orElseThrow();

        assertThat(reloaded.getContextType()).isEqualTo(ConversationContextType.REQUEST);
        assertThat(store.findMessages(reloaded.getId(), false))
                .extracting(MessageEntity::getBody)
                .containsExactly("Synthetic first message");
        assertThat(store.findMessages(reloaded.getId(), true))
                .extracting(MessageEntity::getBody)
                .containsExactly("Synthetic first message", "Synthetic internal note");
    }

    @Test
    @DisplayName("TC-MSG-001: anonymous CONTACT author can be stored without identity PII")
    void persistsAnonymousContactMessageWithoutAuthorId() {
        ConversationEntity contact = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
        store.saveConversation(contact);
        store.saveMessage(new MessageEntity(UUID.randomUUID(), contact, null,
                MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, "Synthetic contact message", NOW));
        entityManager.flush();
        entityManager.clear();

        ConversationEntity reloaded = store.findConversation(contact.getPublicId()).orElseThrow();

        assertThat(store.findMessages(reloaded.getId(), false))
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.getAuthorType()).isEqualTo(MessageAuthorType.ANONYMOUS);
                    assertThat(message.getAuthorId()).isNull();
                    assertThat(message.toString()).doesNotContain("Synthetic contact message");
                });
    }
}
