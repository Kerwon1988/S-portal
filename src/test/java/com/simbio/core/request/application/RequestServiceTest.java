package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.simbiosphere.api.LinkedBusinessQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;

class RequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private final RequestStore store = org.mockito.Mockito.mock(RequestStore.class);
    private final PublicReferenceGenerator references = org.mockito.Mockito.mock(PublicReferenceGenerator.class);
    private final CustomerDirectory customers = org.mockito.Mockito.mock(CustomerDirectory.class);
    private final FileStorage files = org.mockito.Mockito.mock(FileStorage.class);
    private final ConversationApi conversations = org.mockito.Mockito.mock(ConversationApi.class);
    private final LinkedBusinessQuery linkedBusinesses = org.mockito.Mockito.mock(LinkedBusinessQuery.class);
    private final AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final RequestService service = new RequestService(store, references, customers, files,
            conversations, linkedBusinesses, audit, events, Clock.fixed(NOW, ZoneOffset.UTC));
    private final AuthenticatedUser client = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
    private final CustomerAccountSummary owner = new CustomerAccountSummary(
            UUID.randomUUID(), "INDIVIDUAL", "Synthetic owner", "ACTIVE", NOW);

    @BeforeEach void transaction() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test
    void createsOwnedCustomDevelopmentAndPublishesAfterCommit() {
        when(customers.findOwnedBy(client.userId())).thenReturn(Optional.of(owner));
        when(store.categoryExists("WEB_APPLICATION")).thenReturn(true);
        when(references.next()).thenReturn("SOL-2026-000001");
        when(store.save(any(RequestEntity.class))).thenAnswer(invocation -> {
            RequestEntity entity = invocation.getArgument(0);
            var id = RequestEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(entity, 1L);
            return entity;
        });
        when(store.saveHistory(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(client, command(RequestType.CUSTOM_DEVELOPMENT));

        assertThat(result.reference()).isEqualTo("SOL-2026-000001");
        verify(conversations).openRequestConversation(client, result.publicId());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void hidesAnotherOwnersRequest() {
        when(customers.findOwnedBy(client.userId())).thenReturn(Optional.of(owner));
        RequestEntity foreign = request(UUID.randomUUID(), UUID.randomUUID());
        when(store.findByPublicId(foreign.getPublicId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.ownedDetail(client, foreign.getPublicId()))
                .isInstanceOf(RequestNotFoundException.class);
    }

    @Test
    void foreignOwnerCannotAttachOrDownloadARequestFile() {
        when(customers.findOwnedBy(client.userId())).thenReturn(Optional.of(owner));
        RequestEntity foreign = request(UUID.randomUUID(), UUID.randomUUID());
        when(store.findByPublicId(foreign.getPublicId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.attach(client, foreign.getPublicId(),
                new java.io.ByteArrayInputStream(new byte[] {1}), "proof.pdf", 1))
                .isInstanceOf(RequestNotFoundException.class);
        assertThatThrownBy(() -> service.downloadAttachment(client, foreign.getPublicId(), UUID.randomUUID()))
                .isInstanceOf(RequestNotFoundException.class);
        verify(files, never()).store(any(), any(), any(long.class), any());
        verify(files, never()).open(any(), any());
    }

    @Test
    void ownerInformationMovesPendingRequestBackToAnalysis() throws Exception {
        when(customers.findOwnedBy(client.userId())).thenReturn(Optional.of(owner));
        RequestEntity request = request(owner.publicId(), client.userId());
        var id = RequestEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(request, 7L);
        request.transition(com.simbio.core.request.api.RequestStatus.EN_ANALISIS, null, null);
        request.transition(com.simbio.core.request.api.RequestStatus.PENDIENTE_INFO_CLIENTE,
                "Synthetic staff question", null);
        when(store.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));
        var conversation = new ConversationView(UUID.randomUUID(), ConversationContextType.REQUEST,
                ConversationStatus.OPEN, NOW, java.util.List.of());
        when(conversations.openRequestConversation(client, request.getPublicId())).thenReturn(conversation);

        service.addInformation(client, request.getPublicId(), "Synthetic public answer");

        assertThat(request.getStatus()).isEqualTo(com.simbio.core.request.api.RequestStatus.EN_ANALISIS);
        verify(conversations).postMessage(client, conversation.publicId(), MessageVisibility.PUBLIC,
                "Synthetic public answer");
    }

    @Test
    void validatesLinkedBusinessOwnershipForModuleSuggestion() {
        when(customers.findOwnedBy(client.userId())).thenReturn(Optional.of(owner));
        when(store.categoryExists("WEB_APPLICATION")).thenReturn(true);
        UUID foreignBusiness = UUID.randomUUID();
        CreateRequestCommand command = new CreateRequestCommand(RequestType.SIMBIOSPHERE_MODULE,
                "WEB_APPLICATION", "Synthetic module", "Synthetic description", "Goal", "Impact",
                "module", foreignBusiness, null, null);
        when(linkedBusinesses.requireOwnedBy(client.userId(), foreignBusiness))
                .thenThrow(new IllegalArgumentException("not owned"));

        assertThatThrownBy(() -> service.create(client, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not owned");
        verify(store, never()).save(any(RequestEntity.class));
    }

    @Test
    void rejectsEmployeeFromClientUseCases() {
        var employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());
        assertThatThrownBy(() -> service.listOwned(employee, null, null, null, null, null,
                com.simbio.core.shared.paging.PageQuery.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private CreateRequestCommand command(RequestType type) {
        return new CreateRequestCommand(type, "WEB_APPLICATION", "Synthetic title", "Synthetic description",
                "Synthetic goal", null, null, null, null, null);
    }

    private static RequestEntity request(UUID ownerAccountId, UUID ownerUserId) {
        return new RequestEntity(UUID.randomUUID(), "SOL-2026-000099", RequestType.CUSTOM_DEVELOPMENT,
                "WEB_APPLICATION", "Synthetic title", "Synthetic description", null, null, null,
                null, null, ownerAccountId, ownerUserId, NOW);
    }
}
