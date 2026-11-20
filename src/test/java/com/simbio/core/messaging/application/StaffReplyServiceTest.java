package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.config.MessagingContactProperties;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class StaffReplyServiceTest {

    private ConversationApi conversations;
    private ConversationStore store;
    private FileStorage files;
    private StaffReplyService service;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        conversations = mock(ConversationApi.class);
        store = mock(ConversationStore.class);
        files = mock(FileStorage.class);
        service = new StaffReplyService(conversations, store, files,
                new MessagingContactProperties("es", "en", 1, 2, 1024, 2048, 8192),
                Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneOffset.UTC));
        actor = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.INBOX_MANAGE));
    }

    @Test
    void storesOnlyCleanAllowedAttachmentAgainstTheNewMessage() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MessageView view = new MessageView(messageId, MessageAuthorType.EMPLOYEE,
                MessageVisibility.PUBLIC, "reply", Instant.now());
        MessageEntity entity = new MessageEntity(messageId,
                new ConversationEntity(conversationId,
                        com.simbio.core.messaging.api.ConversationContextType.CONTACT,
                        UUID.randomUUID(), null, Instant.now()),
                actor.userId(), MessageAuthorType.EMPLOYEE, MessageVisibility.PUBLIC,
                "reply", Instant.now());
        ReflectionTestUtils.setField(entity, "id", 42L);
        StoredFile stored = new StoredFile(UUID.randomUUID(), "image/png", png.length, ScanState.CLEAN);
        when(conversations.postMessage(actor, conversationId, MessageVisibility.PUBLIC, "reply"))
                .thenReturn(view);
        when(store.findMessage(messageId)).thenReturn(Optional.of(entity));
        when(files.store(any(ByteArrayInputStream.class), any(), any(Long.class), any()))
                .thenReturn(stored);

        service.reply(actor, conversationId, MessageVisibility.PUBLIC, "reply",
                List.of(new ContactAttachmentInput("pixel.png", png.length, png, null)));

        verify(store).saveMessageAttachment(any());
    }

    @Test
    void rejectsAttachmentCountOrFalseMimeWithoutStoring() {
        UUID conversationId = UUID.randomUUID();
        MessageView view = new MessageView(UUID.randomUUID(), MessageAuthorType.EMPLOYEE,
                MessageVisibility.PUBLIC, "reply", Instant.now());
        when(conversations.postMessage(any(), any(), any(), any())).thenReturn(view);
        var input = new ContactAttachmentInput("payload.pdf", 3, new byte[] {'e','x','e'}, null);

        assertThatThrownBy(() -> service.reply(actor, conversationId, MessageVisibility.PUBLIC,
                "reply", List.of(input, input, input))).isInstanceOf(ContactSubmissionRejectedException.class);

        assertThatThrownBy(() -> service.reply(actor, conversationId, MessageVisibility.PUBLIC,
                "reply", List.of(input))).isInstanceOf(ContactSubmissionRejectedException.class);
    }
}
