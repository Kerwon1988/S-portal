package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.config.MessagingContactProperties;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;
import com.simbio.core.shared.spi.AntiBotVerification;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock private ConversationStore store;
    @Mock private FileStorage files;
    @Mock private SensitiveDataProtector protector;
    @Mock private AntiBotVerification antiBot;
    @Mock private ApplicationEventPublisher events;

    private ContactSubmissionService service;
    private MessagingContactProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MessagingContactProperties(
                "Texto configurado", "Configured text", 7, 3, 10_240, 20_480, 8_192);
        service = new ContactSubmissionService(store, files, protector, antiBot, properties, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(antiBot.verify(any(), any(), any(Boolean.class))).thenReturn(true);
        when(protector.protect(any())).thenReturn("ciphertext");
        when(store.saveConversation(any())).thenAnswer(invocation -> {
            ConversationEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 10L);
            return value;
        });
        when(store.saveMessage(any())).thenAnswer(invocation -> {
            MessageEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 20L);
            return value;
        });
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void persistsContactAndPublicMessageAndPublishesOnlyMessagingEventAfterCommit() {
        ContactSubmissionResult result = service.submit(command(false, 7, List.of()));

        ArgumentCaptor<ConversationEntity> conversation = ArgumentCaptor.forClass(ConversationEntity.class);
        ArgumentCaptor<MessageEntity> message = ArgumentCaptor.forClass(MessageEntity.class);
        ArgumentCaptor<ContactSubmissionEntity> submission = ArgumentCaptor.forClass(ContactSubmissionEntity.class);
        verify(store).saveConversation(conversation.capture());
        verify(store).saveMessage(message.capture());
        verify(store).saveContactSubmission(submission.capture());
        assertThat(conversation.getValue().getContextType()).isEqualTo(ConversationContextType.CONTACT);
        assertThat(message.getValue().getVisibility()).isEqualTo(MessageVisibility.PUBLIC);
        assertThat(message.getValue().getBody()).isEqualTo("Synthetic contact message");
        assertThat(submission.getValue().getEmailEncrypted()).doesNotContain("person@example.test");
        verify(events, never()).publishEvent(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(events).publishEvent(any(MessageReceived.class));
        assertThat(result.conversationPublicId()).isEqualTo(conversation.getValue().getPublicId());
    }

    @Test
    void rejectsFailedAntiBotWithoutPersistingContent() {
        when(antiBot.verify(any(), any(), any(Boolean.class))).thenReturn(false);

        assertThatThrownBy(() -> service.submit(command(false, 7, List.of())))
                .isInstanceOf(ContactSubmissionRejectedException.class);

        verify(store, never()).saveConversation(any());
        verify(store, never()).saveMessage(any());
        verify(files, never()).store(any(), any(), anyLong(), any());
    }

    @Test
    void rejectsMissingOrStaleConsentWithoutPersistingContent() {
        assertThatThrownBy(() -> service.submit(command(false, 6, List.of())))
                .isInstanceOf(ContactSubmissionRejectedException.class);
        properties = new MessagingContactProperties("", "", 0, 3, 10240, 20480, 8192);
        service = new ContactSubmissionService(store, files, protector, antiBot, properties, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.submit(command(false, 0, List.of())))
                .isInstanceOf(ContactUnavailableException.class);

        verify(store, never()).saveConversation(any());
    }

    @Test
    void rejectsFalseMimePathTraversalAndAttachmentCount() {
        ContactAttachmentInput falseMime = new ContactAttachmentInput(
                "payload.pdf", 3, new byte[] {'e', 'x', 'e'}, null);
        ContactAttachmentInput traversal = new ContactAttachmentInput(
                "../payload.pdf", 1, new byte[] {'x'}, null);

        assertThatThrownBy(() -> service.submit(command(false, 7, List.of(falseMime))))
                .isInstanceOf(ContactSubmissionRejectedException.class);
        assertThatThrownBy(() -> service.submit(command(false, 7, List.of(traversal))))
                .isInstanceOf(ContactSubmissionRejectedException.class);
        assertThatThrownBy(() -> service.submit(command(false, 7, List.of(falseMime, falseMime, falseMime, falseMime))))
                .isInstanceOf(ContactSubmissionRejectedException.class);

        verify(files, never()).store(any(), any(), anyLong(), any());
    }

    @Test
    void rejectsAudioLongerThanOneHundredTwentySeconds() {
        byte[] mp4 = new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p', 'M', '4', 'A', ' ', 0, 0, 0, 0,
                'M', '4', 'A', ' ', 'i', 's', 'o', 'm'};
        ContactAttachmentInput audio = new ContactAttachmentInput("note.m4a", mp4.length, mp4, 121);

        assertThatThrownBy(() -> service.submit(command(false, 7, List.of(audio))))
                .isInstanceOf(ContactSubmissionRejectedException.class);
        verify(files, never()).store(any(), any(), anyLong(), any());
    }

    @Test
    void rejectsAttachmentUnlessFileModuleReportsClean() {
        byte[] pdf = "%PDF-1.4\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        ContactAttachmentInput attachment = new ContactAttachmentInput("note.pdf", pdf.length, pdf, null);
        when(files.store(any(), any(), anyLong(), any())).thenReturn(
                new StoredFile(UUID.randomUUID(), "application/pdf", pdf.length, ScanState.QUARANTINED));

        assertThatThrownBy(() -> service.submit(command(false, 7, List.of(attachment))))
                .isInstanceOf(ContactSubmissionRejectedException.class);

        verify(store, never()).saveMessageAttachment(any());
        verify(events, never()).publishEvent(any());
    }

    private static ContactSubmissionCommand command(boolean consent, int version,
            List<ContactAttachmentInput> attachments) {
        return new ContactSubmissionCommand("Synthetic Person", "person@example.test", "+34123456789",
                "Synthetic reason", "Synthetic contact message", "es", !consent, version,
                "proof", consent ? "https://trap.invalid" : "", !consent, null, attachments);
    }
}
