package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageAttachmentEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@ExtendWith(MockitoExtension.class)
class ContactAttachmentServiceTest {

    @Mock private ConversationStore store;
    @Mock private FileStorage files;

    private ContactAttachmentService service;
    private ConversationEntity conversation;

    @BeforeEach
    void setUp() {
        service = new ContactAttachmentService(store, files);
        conversation = new ConversationEntity(UUID.randomUUID(), ConversationContextType.CONTACT,
                UUID.randomUUID(), null, Instant.parse("2026-11-20T00:00:00Z"));
        ReflectionTestUtils.setField(conversation, "id", 9L);
    }

    @Test
    void deniesAttachmentDownloadWithoutInboxPermission() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.download(employee, conversation.getPublicId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        verify(files, never()).open(any(), any());
    }

    @Test
    void deniesAttachmentBelongingToAnotherConversation() {
        AuthenticatedUser manager = manager();
        UUID requestedFile = UUID.randomUUID();
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(store.findMessageAttachment(requestedFile)).thenReturn(Optional.of(
                new MessageAttachmentEntity(99L, requestedFile, "application/pdf", 32,
                        Instant.parse("2026-11-20T00:00:00Z"))));
        when(store.findMessageAttachments(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.download(manager, conversation.getPublicId(), requestedFile))
                .isInstanceOf(ContactAttachmentNotFoundException.class);
        verify(files, never()).open(any(), any());
    }

    @Test
    void listsOnlyContactAttachmentsForAuthorizedStaff() {
        AuthenticatedUser manager = manager();
        UUID fileId = UUID.randomUUID();
        when(store.findConversation(conversation.getPublicId())).thenReturn(Optional.of(conversation));
        when(store.findMessageAttachments(9L)).thenReturn(List.of(
                new MessageAttachmentEntity(1L, fileId, "application/pdf", 32,
                        Instant.parse("2026-11-20T00:00:00Z"))));

        assertThat(service.list(manager, conversation.getPublicId()))
                .extracting(ContactAttachmentView::filePublicId)
                .containsExactly(fileId);
    }

    private static AuthenticatedUser manager() {
        return new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of(Permission.INBOX_MANAGE));
    }
}
