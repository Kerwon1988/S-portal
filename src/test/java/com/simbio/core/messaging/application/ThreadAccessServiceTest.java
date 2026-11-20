package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.ThreadAccessTokenEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class ThreadAccessServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T17:00:00Z");

    private ConversationStore conversations;
    private ThreadAccessTokenStore tokens;
    private ApplicationEventPublisher events;
    private ConversationEntity conversation;
    private ThreadAccessService service;

    @BeforeEach
    void setUp() {
        conversations = mock(ConversationStore.class);
        tokens = mock(ThreadAccessTokenStore.class);
        events = mock(ApplicationEventPublisher.class);
        conversation = new ConversationEntity(UUID.randomUUID(), ConversationContextType.CONTACT,
                UUID.randomUUID(), null, NOW);
        when(conversations.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ThreadAccessService(conversations, tokens, mock(AuditRecorder.class), events,
                Clock.fixed(NOW, ZoneOffset.UTC), predictableRandom());
    }

    @Test
    void issueCreatesAtLeast128BitsAndStoresOnlyHash() {
        var issued = service.issue(employee(), conversation.getPublicId());

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.rawToken().length()).isGreaterThanOrEqualTo(40);
        verify(tokens).save(org.mockito.ArgumentMatchers.argThat(token -> {
            try {
                String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(issued.rawToken().getBytes(StandardCharsets.UTF_8)));
                assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(ThreadAccessService.DEFAULT_TTL));
                return expected.equals(getHash(token));
            } catch (Exception exception) {
                return false;
            }
        }));
    }

    @Test
    void expiredOrReplayedTokenCannotEnumerateConversation() {
        String raw = "a".repeat(43);
        ThreadAccessTokenEntity token = new ThreadAccessTokenEntity(UUID.randomUUID(), conversation,
                hash(raw), NOW.minusSeconds(1));
        when(tokens.findByTokenHash(hash(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.read(raw)).isInstanceOf(ConversationNotFoundException.class);
        verify(conversations, never()).findMessages(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void replyConsumesTokenAndPublishesOnlyIdentifierEvent() {
        String raw = "b".repeat(43);
        ThreadAccessTokenEntity token = new ThreadAccessTokenEntity(UUID.randomUUID(), conversation,
                hash(raw), NOW.plusSeconds(60));
        when(tokens.findByTokenHash(hash(raw))).thenReturn(Optional.of(token));
        when(conversations.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reply(raw, "Follow-up");

        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        verify(events).publishEvent(any(MessageReceived.class));
        assertThatThrownBy(() -> service.reply(raw, "Replay")).isInstanceOf(ConversationNotFoundException.class);
    }

    private static AuthenticatedUser employee() {
        return new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of(Permission.INBOX_MANAGE));
    }

    private static SecureRandom predictableRandom() {
        return new SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                for (int index = 0; index < bytes.length; index++) {
                    bytes[index] = (byte) (index + 1);
                }
            }
        };
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String getHash(ThreadAccessTokenEntity token) {
        try {
            var field = ThreadAccessTokenEntity.class.getDeclaredField("tokenHash");
            field.setAccessible(true);
            return (String) field.get(token);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
