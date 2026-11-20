package com.simbio.core.messaging.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.config.MessagingContactProperties;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.spi.AntiBotVerification;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class ContactSubmissionService {

    private static final int MAX_NAME = 120;
    private static final int MAX_EMAIL = 320;
    private static final int MAX_PHONE = 24;
    private static final int MAX_REASON = 160;
    private static final int MAX_MESSAGE = 10_000;
    private static final int MAX_AUDIO_SECONDS = 120;
    private static final Map<String, Set<String>> ALLOWED_ATTACHMENTS = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/png", Set.of("png"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/webp", Set.of("webp"),
            "audio/webm", Set.of("webm"),
            "video/webm", Set.of("webm"),
            "audio/ogg", Set.of("ogg", "oga"),
            "audio/mp4", Set.of("m4a", "mp4"),
            "video/mp4", Set.of("mp4"));

    private final ConversationStore store;
    private final FileStorage files;
    private final SensitiveDataProtector dataProtector;
    private final AntiBotVerification antiBot;
    private final MessagingContactProperties properties;
    private final ApplicationEventPublisher events;
    private final Tika tika = new Tika();
    private final Clock clock;

    @Autowired
    public ContactSubmissionService(ConversationStore store, FileStorage files,
            SensitiveDataProtector dataProtector, AntiBotVerification antiBot,
            MessagingContactProperties properties, ApplicationEventPublisher events) {
        this(store, files, dataProtector, antiBot, properties, events, Clock.systemUTC());
    }

    ContactSubmissionService(ConversationStore store, FileStorage files,
            SensitiveDataProtector dataProtector, AntiBotVerification antiBot,
            MessagingContactProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.files = Objects.requireNonNull(files);
        this.dataProtector = Objects.requireNonNull(dataProtector);
        this.antiBot = Objects.requireNonNull(antiBot);
        this.properties = Objects.requireNonNull(properties);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public ContactSubmissionResult submit(ContactSubmissionCommand command) {
        Objects.requireNonNull(command);
        String locale = normalizeLocale(command.locale());
        if (!properties.available(locale)) {
            throw new ContactUnavailableException();
        }
        if (!command.consentAccepted() || command.consentVersion() != properties.consentVersion()
                || !antiBot.verify(command.altchaPayload(), command.honeypot(),
                        command.accessibleConfirmation())) {
            throw new ContactSubmissionRejectedException();
        }

        String name = required(command.personName(), MAX_NAME);
        String email = required(command.email(), MAX_EMAIL).toLowerCase(Locale.ROOT);
        String phone = optional(command.phone(), MAX_PHONE);
        String reason = required(command.reason(), MAX_REASON);
        String body = required(command.message(), MAX_MESSAGE);
        requireEmail(email);
        List<ValidatedAttachment> attachments = validateAttachments(command.attachments());

        Instant now = Instant.now(clock);
        UUID conversationId = UUID.randomUUID();
        ConversationEntity conversation = store.saveConversation(new ConversationEntity(
                conversationId, ConversationContextType.CONTACT, UUID.randomUUID(),
                command.authenticatedClientId(), now));
        store.saveContactSubmission(new ContactSubmissionEntity(
                UUID.randomUUID(), conversation.getId(), dataProtector.protect(name),
                dataProtector.protect(email), phone == null ? null : dataProtector.protect(phone),
                reason, locale, properties.consentVersion(), sha256(properties.consentText(locale)), now));
        MessageEntity message = store.saveMessage(new MessageEntity(
                UUID.randomUUID(), conversation, command.authenticatedClientId(),
                command.authenticatedClientId() == null
                        ? MessageAuthorType.ANONYMOUS : MessageAuthorType.CLIENT,
                MessageVisibility.PUBLIC, body, now));

        for (ValidatedAttachment attachment : attachments) {
            StoredFile stored = files.store(new ByteArrayInputStream(attachment.content()),
                    attachment.safeName(), attachment.content().length, FilePurpose.ATTACHMENT);
            if (stored.scanState() != ScanState.CLEAN) {
                throw new ContactSubmissionRejectedException();
            }
            store.saveMessageAttachment(new MessageAttachmentEntity(message.getId(), stored.publicId(),
                    stored.detectedMime(), stored.sizeBytes(), now));
        }
        publishAfterCommit(new MessageReceived(conversation.getPublicId(), message.getPublicId()));
        return new ContactSubmissionResult(conversation.getPublicId(), message.getPublicId());
    }

    private List<ValidatedAttachment> validateAttachments(List<ContactAttachmentInput> inputs) {
        if (inputs.size() > properties.maxAttachments()) {
            throw new ContactSubmissionRejectedException();
        }
        List<ValidatedAttachment> validated = new java.util.ArrayList<>();
        long total = 0;
        for (ContactAttachmentInput input : inputs) {
            if (input == null || input.content() == null || input.content().length == 0
                    || input.declaredSize() != input.content().length
                    || input.content().length > properties.maxAttachmentBytes()
                    || input.submittedName() == null || input.submittedName().isBlank()
                    || input.submittedName().length() > 255
                    || input.submittedName().contains("/") || input.submittedName().contains("\\")
                    || input.submittedName().contains("..")) {
                throw new ContactSubmissionRejectedException();
            }
            total += input.content().length;
            if (total > properties.maxTotalAttachmentBytes()) {
                throw new ContactSubmissionRejectedException();
            }
            String extension = extension(input.submittedName());
            String mime;
            try {
                mime = tika.detect(new ByteArrayInputStream(input.content()));
            } catch (IOException exception) {
                throw new ContactSubmissionRejectedException();
            }
            Set<String> allowedExtensions = ALLOWED_ATTACHMENTS.get(mime);
            if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
                throw new ContactSubmissionRejectedException();
            }
            boolean audio = mime.startsWith("audio/") || mime.equals("video/webm") || mime.equals("video/mp4");
            Integer duration = input.audioDurationSeconds();
            if (audio && (duration == null || duration < 1 || duration > MAX_AUDIO_SECONDS
                    || input.content().length > properties.maxAudioBytes())) {
                throw new ContactSubmissionRejectedException();
            }
            validated.add(new ValidatedAttachment("attachment." + extension, input.content()));
        }
        return List.copyOf(validated);
    }

    private static String required(String value, int maximum) {
        String normalized = optional(value, maximum);
        if (normalized == null) throw new ContactSubmissionRejectedException();
        return normalized;
    }

    private static String optional(String value, int maximum) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > maximum) throw new ContactSubmissionRejectedException();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void requireEmail(String email) {
        if (!email.matches("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$")) {
            throw new ContactSubmissionRejectedException();
        }
    }

    private static String normalizeLocale(String locale) {
        return "en".equals(locale) ? "en" : "es";
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) throw new ContactSubmissionRejectedException();
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,8}")) throw new ContactSubmissionRejectedException();
        return extension;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Contact events require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }

    private record ValidatedAttachment(String safeName, byte[] content) {
        private ValidatedAttachment {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
