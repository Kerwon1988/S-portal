package com.simbio.core.messaging.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.messaging.application.ConversationStore;
import com.simbio.core.messaging.application.InboxAssignmentFilter;
import com.simbio.core.messaging.application.InboxFilters;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
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

    @Test
    @DisplayName("TC-MSG-007: inbox filters only public messages and authorized contexts")
    void filtersUnreadAssignmentsAndPublicMessageSearch() {
        UUID actorId = UUID.randomUUID();
        ConversationEntity assignedContact = contactConversation();
        assignedContact.assignTo(actorId);
        store.saveConversation(assignedContact);
        store.saveMessage(new MessageEntity(UUID.randomUUID(), assignedContact, null,
                MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, "Synthetic inbox search phrase", NOW));

        ConversationEntity unassignedContact = contactConversation();
        store.saveConversation(unassignedContact);
        store.saveMessage(new MessageEntity(UUID.randomUUID(), unassignedContact, UUID.randomUUID(),
                MessageAuthorType.EMPLOYEE, MessageVisibility.INTERNAL, "Synthetic inbox search phrase", NOW));

        ConversationEntity requestConversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.REQUEST, UUID.randomUUID(), UUID.randomUUID(), NOW);
        store.saveConversation(requestConversation);
        store.saveMessage(new MessageEntity(UUID.randomUUID(), requestConversation, null,
                MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, "Synthetic inbox search phrase", NOW));
        entityManager.flush();
        entityManager.clear();

        var unreadAssigned = store.searchInbox(
                new InboxFilters(null, ConversationStatus.OPEN, InboxAssignmentFilter.MINE, true, ""),
                actorId, false, PageRequest.of(0, 10));
        var publicSearch = store.searchInbox(
                new InboxFilters(null, null, InboxAssignmentFilter.ALL, false, "Synthetic inbox search phrase"),
                actorId, false, PageRequest.of(0, 10));

        assertThat(unreadAssigned.getContent())
                .extracting("publicId")
                .containsExactly(assignedContact.getPublicId());
        assertThat(publicSearch.getContent())
                .extracting("publicId")
                .containsExactly(assignedContact.getPublicId());
        assertThat(publicSearch.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-MSG-007: inbox pagination has stable tie-breaking")
    void inboxPagesHaveStableTieBreaking() {
        UUID actorId = UUID.randomUUID();
        java.util.List<UUID> expectedIds = new java.util.ArrayList<>();
        for (int index = 0; index < 3; index++) {
            ConversationEntity conversation = contactConversation();
            store.saveConversation(conversation);
            store.saveMessage(new MessageEntity(UUID.randomUUID(), conversation, null,
                    MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, "Synthetic stable page phrase", NOW));
            expectedIds.add(conversation.getPublicId());
        }
        entityManager.flush();
        entityManager.clear();

        var firstPage = store.searchInbox(
                new InboxFilters(ConversationContextType.CONTACT, null, InboxAssignmentFilter.ALL,
                        false, "Synthetic stable page phrase"),
                actorId, false, PageRequest.of(0, 2));
        var secondPage = store.searchInbox(
                new InboxFilters(ConversationContextType.CONTACT, null, InboxAssignmentFilter.ALL,
                        false, "Synthetic stable page phrase"),
                actorId, false, PageRequest.of(1, 2));

        java.util.List<UUID> resultIds = new java.util.ArrayList<>();
        firstPage.getContent().forEach(item -> resultIds.add(item.publicId()));
        secondPage.getContent().forEach(item -> resultIds.add(item.publicId()));
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(resultIds).containsExactlyInAnyOrderElementsOf(expectedIds).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("TC-MSG-012: AI_AGENT messages persist only as internal drafts")
    void persistsAiAgentDraftAsInternal() {
        ConversationEntity conversation = contactConversation();
        store.saveConversation(conversation);
        store.saveMessage(new MessageEntity(
                UUID.randomUUID(), conversation, null, MessageAuthorType.AI_AGENT,
                MessageVisibility.INTERNAL, "Synthetic AI draft", NOW));
        entityManager.flush();
        entityManager.clear();

        var messages = store.findMessages(conversation.getId(), true);

        assertThat(messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.getAuthorType()).isEqualTo(MessageAuthorType.AI_AGENT);
                    assertThat(message.getAuthorId()).isNull();
                    assertThat(message.getVisibility()).isEqualTo(MessageVisibility.INTERNAL);
                    assertThat(message.toString()).doesNotContain("Synthetic AI draft");
                });
        assertThat(store.findMessages(conversation.getId(), false)).isEmpty();
    }

    private static ConversationEntity contactConversation() {
        return new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW);
    }
}
