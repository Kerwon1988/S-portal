package com.simbio.core.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;

class MessageEntityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-20T08:00:00Z");

    @Test
    void exposesReadStateAndDoesNotIncludeBodyInStringRepresentation() {
        UUID publicId = UUID.randomUUID();
        ConversationEntity conversation = conversation();
        String syntheticBody = "Synthetic private message";
        MessageEntity message = new MessageEntity(publicId, conversation, UUID.randomUUID(),
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, syntheticBody, CREATED_AT);

        assertThat(message.getId()).isNull();
        assertThat(message.getPublicId()).isEqualTo(publicId);
        assertThat(message.getConversation()).isSameAs(conversation);
        assertThat(message.getAuthorType()).isEqualTo(MessageAuthorType.CLIENT);
        assertThat(message.getVisibility()).isEqualTo(MessageVisibility.PUBLIC);
        assertThat(message.getBody()).isEqualTo(syntheticBody);
        assertThat(message.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(message.getReadAt()).isNull();
        assertThat(message.toString()).contains(publicId.toString(), "CLIENT", "PUBLIC")
                .doesNotContain(syntheticBody);

        Instant readAt = CREATED_AT.plusSeconds(60);
        message.markRead(readAt);
        assertThat(message.getReadAt()).isEqualTo(readAt);
        assertThatThrownBy(() -> message.markRead(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void anonymousSystemAndAiMessagesCannotHaveUserIdentity() {
        for (MessageAuthorType authorType : new MessageAuthorType[] {
                MessageAuthorType.ANONYMOUS, MessageAuthorType.SYSTEM, MessageAuthorType.AI_AGENT}) {
            assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation(), UUID.randomUUID(),
                    authorType, MessageVisibility.INTERNAL, "Synthetic message", CREATED_AT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not carry a user id");
        }
    }

    @Test
    void authenticatedMessagesRequireAnAuthorIdentity() {
        for (MessageAuthorType authorType : new MessageAuthorType[] {
                MessageAuthorType.CLIENT, MessageAuthorType.EMPLOYEE, MessageAuthorType.ADMIN}) {
            assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation(), null,
                    authorType, MessageVisibility.PUBLIC, "Synthetic message", CREATED_AT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("require an author id");
        }
    }

    @Test
    void rejectsMissingFieldsAndInvalidBodies() {
        ConversationEntity conversation = conversation();
        UUID authorId = UUID.randomUUID();
        assertThatThrownBy(() -> new MessageEntity(null, conversation, authorId,
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, "Synthetic message", CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publicId");
        assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), null, authorId,
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, "Synthetic message", CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("conversation");
        assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation, authorId,
                null, MessageVisibility.PUBLIC, "Synthetic message", CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("authorType");
        assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation, authorId,
                MessageAuthorType.CLIENT, null, "Synthetic message", CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("visibility");
        assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation, authorId,
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, "Synthetic message", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdAt");

        assertInvalidBody(null);
        assertInvalidBody("  ");
        assertInvalidBody("x".repeat(10_001));
    }

    private static void assertInvalidBody(String body) {
        assertThatThrownBy(() -> new MessageEntity(UUID.randomUUID(), conversation(), UUID.randomUUID(),
                MessageAuthorType.CLIENT, MessageVisibility.PUBLIC, body, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 to 10000 characters");
    }

    private static ConversationEntity conversation() {
        return new ConversationEntity(UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(),
                null, CREATED_AT);
    }
}
