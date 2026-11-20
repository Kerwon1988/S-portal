package com.simbio.core.messaging.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.api.MessageAttachmentView;
import com.simbio.core.messaging.api.MessageView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.messaging.domain.ThreadAccessTokenEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class ThreadAccessService {

    public static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final int TOKEN_BYTES = 32;

    private final ConversationStore store;
    private final ThreadAccessTokenStore tokens;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final FileStorage files;
    private final Clock clock;
    private final SecureRandom random;

    @Autowired
    public ThreadAccessService(
            ConversationStore store,
            ThreadAccessTokenStore tokens,
            AuditRecorder audit,
            ApplicationEventPublisher events,
            FileStorage files) {
        this(store, tokens, audit, events, files, Clock.systemUTC(), new SecureRandom());
    }

    ThreadAccessService(
            ConversationStore store,
            ThreadAccessTokenStore tokens,
            AuditRecorder audit,
            ApplicationEventPublisher events,
            Clock clock,
            SecureRandom random) {
        this(store, tokens, audit, events, null, clock, random);
    }

    ThreadAccessService(
            ConversationStore store,
            ThreadAccessTokenStore tokens,
            AuditRecorder audit,
            ApplicationEventPublisher events,
            FileStorage files,
            Clock clock,
            SecureRandom random) {
        this.store = Objects.requireNonNull(store);
        this.tokens = Objects.requireNonNull(tokens);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        this.files = files;
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
    }

    @Transactional
    public IssuedThreadAccess issue(AuthenticatedUser actor, UUID conversationId) {
        requireInboxManager(actor);
        ConversationEntity conversation = store.findConversation(conversationId)
                .filter(value -> value.getContextType() == ConversationContextType.CONTACT)
                .orElseThrow(ConversationNotFoundException::new);
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ThreadAccessTokenEntity token = tokens.save(new ThreadAccessTokenEntity(
                UUID.randomUUID(), conversation, hash(raw), Instant.now(clock).plus(DEFAULT_TTL)));
        audit.record(new AuditRecord(actor.userId(), "THREAD_ACCESS_ISSUED", "CONVERSATION",
                conversation.getPublicId(), token.getPublicId().toString()));
        return new IssuedThreadAccess(raw, token.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ConversationView read(String rawToken) {
        ThreadAccessTokenEntity token = usable(rawToken);
        ConversationEntity conversation = token.getConversation();
        List<MessageView> messages = store.findMessages(conversation.getId(), false).stream()
                .map(message -> new MessageView(message.getPublicId(), message.getAuthorType(),
                        message.getVisibility(), message.getBody(), message.getCreatedAt(),
                        store.findMessageAttachments(conversation.getId()).stream()
                                .filter(attachment -> attachment.getMessageId().equals(message.getId()))
                                .map(attachment -> new MessageAttachmentView(attachment.getFilePublicId(),
                                        attachment.getDetectedMime(), attachment.getSizeBytes()))
                                .toList()))
                .toList();
        return new ConversationView(conversation.getPublicId(), conversation.getContextType(),
                conversation.getStatus(), conversation.getCreatedAt(), messages);
    }

    @Transactional
    public MessageView reply(String rawToken, String body) {
        if (body == null || body.isBlank() || body.length() > 10_000) {
            throw new IllegalArgumentException("Message body must contain 1 to 10000 characters");
        }
        ThreadAccessTokenEntity token = usable(rawToken);
        ConversationEntity conversation = token.getConversation();
        if (conversation.getContextType() != ConversationContextType.CONTACT) {
            throw new ConversationNotFoundException();
        }
        token.consume(Instant.now(clock));
        MessageEntity message = store.saveMessage(new MessageEntity(UUID.randomUUID(), conversation,
                null, MessageAuthorType.ANONYMOUS, MessageVisibility.PUBLIC, body, Instant.now(clock)));
        MessageReceived event = new MessageReceived(conversation.getPublicId(), message.getPublicId());
        publishAfterCommit(event);
        return new MessageView(message.getPublicId(), message.getAuthorType(), message.getVisibility(),
                message.getBody(), message.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ContactAttachmentDownload download(String rawToken, UUID filePublicId) {
        ThreadAccessTokenEntity token = usable(rawToken);
        var attachment = store.findMessageAttachment(Objects.requireNonNull(filePublicId, "filePublicId"))
                .filter(value -> store.findMessageAttachments(token.getConversation().getId()).stream()
                        .anyMatch(candidate -> candidate.getFilePublicId().equals(value.getFilePublicId())))
                .orElseThrow(ContactAttachmentNotFoundException::new);
        if (files == null) {
            throw new IllegalStateException("File storage is required for attachment downloads");
        }
        var input = files.open(filePublicId, requested -> requested.equals(attachment.getFilePublicId()));
        return new ContactAttachmentDownload(attachment.getDetectedMime(), attachment.getSizeBytes(), input);
    }

    private ThreadAccessTokenEntity usable(String rawToken) {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 256) {
            throw new ConversationNotFoundException();
        }
        ThreadAccessTokenEntity token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(ConversationNotFoundException::new);
        if (!token.isUsable(Instant.now(clock))
                || token.getConversation().getContextType() != ConversationContextType.CONTACT) {
            throw new ConversationNotFoundException();
        }
        return token;
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private void publishAfterCommit(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    events.publishEvent(event);
                }
            });
        } else {
            events.publishEvent(event);
        }
    }

    private static void requireInboxManager(AuthenticatedUser actor) {
        if (actor == null || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)
                || !actor.hasPermission(Permission.INBOX_MANAGE)) {
            throw new AccessDeniedException("Inbox management permission required");
        }
    }

    public record IssuedThreadAccess(String rawToken, Instant expiresAt) {
        public IssuedThreadAccess {
            Objects.requireNonNull(rawToken);
            Objects.requireNonNull(expiresAt);
        }
    }
}
