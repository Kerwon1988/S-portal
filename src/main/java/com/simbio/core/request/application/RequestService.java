package com.simbio.core.request.application;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.RequestApi;
import com.simbio.core.request.api.RequestCreated;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestStatusChanged;
import com.simbio.core.request.api.RequestSummary;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestAttachmentEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;
import com.simbio.core.simbiosphere.api.LinkedBusinessQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;

@Service
public class RequestService implements RequestApi {
    public static final int MAX_ATTACHMENTS = 5;
    private static final String MODULE_ALLOWED = "^[A-Za-z0-9_-]{1,80}$";

    private final RequestStore store;
    private final PublicReferenceGenerator references;
    private final CustomerDirectory customers;
    private final FileStorage files;
    private final ConversationApi conversations;
    private final LinkedBusinessQuery linkedBusinesses;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public RequestService(RequestStore store, PublicReferenceGenerator references,
            CustomerDirectory customers, FileStorage files, ConversationApi conversations,
            LinkedBusinessQuery linkedBusinesses, AuditRecorder audit, ApplicationEventPublisher events) {
        this(store, references, customers, files, conversations, linkedBusinesses, audit, events, Clock.systemUTC());
    }

    RequestService(RequestStore store, PublicReferenceGenerator references,
            CustomerDirectory customers, FileStorage files, ConversationApi conversations,
            LinkedBusinessQuery linkedBusinesses, AuditRecorder audit, ApplicationEventPublisher events, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.references = Objects.requireNonNull(references);
        this.customers = Objects.requireNonNull(customers);
        this.files = Objects.requireNonNull(files);
        this.conversations = Objects.requireNonNull(conversations);
        this.linkedBusinesses = Objects.requireNonNull(linkedBusinesses);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public RequestSummary create(AuthenticatedUser actor, CreateRequestCommand command) {
        CustomerAccountSummary owner = requireOwner(actor);
        validate(command);
        if (command.type() == RequestType.SIMBIOSPHERE_MODULE) {
            if (command.linkedBusinessPublicId() == null) {
                throw new IllegalArgumentException("A linked business is required");
            }
            linkedBusinesses.requireOwnedBy(actor.userId(), command.linkedBusinessPublicId());
        }
        RequestEntity request = store.save(new RequestEntity(
                UUID.randomUUID(), references.next(), command.type(), command.category(),
                command.title(), command.description(), command.objective(), command.impact(),
                command.relatedModule(), command.linkedBusinessPublicId(),
                command.estimatedBudget(), command.targetDate(),
                owner.publicId(), actor.userId(), Instant.now(clock)));
        store.saveHistory(new RequestStatusHistoryEntity(request.getId(), null,
                RequestStatus.RECIBIDA, actor.userId(), null, Instant.now(clock)));
        conversations.openRequestConversation(actor, request.getPublicId());
        audit.record(new AuditRecord(actor.userId(), "REQUEST_CREATED", "REQUEST",
                request.getPublicId(), null));
        publishAfterCommit(new RequestCreated(request.getPublicId(), owner.publicId()));
        return summary(request);
    }

    @Transactional(readOnly = true)
    public PageResult<RequestSummary> listOwned(AuthenticatedUser actor, RequestType type,
            RequestStatus status, String reference, Instant createdFrom, Instant createdBefore,
            PageQuery page) {
        CustomerAccountSummary owner = requireOwner(actor);
        var result = store.searchOwned(owner.publicId(), type, status, reference,
                createdFrom, createdBefore, PageRequest.of(page.page(), page.size(),
                        Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PageResult<>(result.map(RequestService::summary).getContent(), page.page(),
                page.size(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RequestDetail ownedDetail(AuthenticatedUser actor, UUID requestPublicId) {
        RequestEntity request = owned(actor, requestPublicId);
        return detail(request);
    }

    @Transactional
    public StoredFile attach(AuthenticatedUser actor, UUID requestPublicId, InputStream input,
            String filename, long size) {
        RequestEntity request = owned(actor, requestPublicId);
        if (store.attachmentsCount(request.getId()) >= MAX_ATTACHMENTS) {
            throw new RequestConflictException("Attachment limit reached");
        }
        StoredFile stored = files.store(input, filename, size, FilePurpose.ATTACHMENT);
        if (stored.scanState() != ScanState.CLEAN) {
            throw new RequestConflictException("Attachment is not clean");
        }
        store.saveAttachment(new RequestAttachmentEntity(
                request.getId(), stored.publicId(), actor.userId(), Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "REQUEST_ATTACHMENT_ADDED", "REQUEST",
                request.getPublicId(), stored.publicId().toString()));
        return stored;
    }

    @Transactional(readOnly = true)
    public InputStream downloadAttachment(AuthenticatedUser actor, UUID requestPublicId, UUID filePublicId) {
        RequestEntity request = owned(actor, requestPublicId);
        store.findAttachment(request.getId(), filePublicId).orElseThrow(RequestNotFoundException::new);
        return files.open(filePublicId, ignored -> true);
    }

    @Transactional
    public void addInformation(AuthenticatedUser actor, UUID requestPublicId, String body) {
        RequestEntity request = owned(actor, requestPublicId);
        if (body == null || body.isBlank() || body.length() > 10_000) {
            throw new IllegalArgumentException("Message body must contain 1 to 10000 characters");
        }
        var conversation = conversations.openRequestConversation(actor, requestPublicId);
        conversations.postMessage(actor, conversation.publicId(), MessageVisibility.PUBLIC, body);
        if (request.getStatus() == RequestStatus.PENDIENTE_INFO_CLIENTE) {
            RequestStatus previous = request.getStatus();
            request.transition(RequestStatus.EN_ANALISIS, "OWNER_INFORMATION_ADDED", null);
            store.save(request);
            store.saveHistory(new RequestStatusHistoryEntity(request.getId(), previous,
                    request.getStatus(), actor.userId(), "OWNER_INFORMATION_ADDED", Instant.now(clock)));
            publishAfterCommit(new RequestStatusChanged(request.getPublicId(), previous, request.getStatus()));
        }
    }

    @Transactional
    public void cancel(AuthenticatedUser actor, UUID requestPublicId) {
        RequestEntity request = owned(actor, requestPublicId);
        RequestStatus previous = request.getStatus();
        if (previous != RequestStatus.RECIBIDA && previous != RequestStatus.EN_ANALISIS) {
            throw new RequestConflictException("This request cannot be cancelled by its owner");
        }
        request.transition(RequestStatus.CANCELADA, "Cancelled by owner", null);
        store.save(request);
        store.saveHistory(new RequestStatusHistoryEntity(request.getId(), previous,
                request.getStatus(), actor.userId(), "OWNER_CANCELLED", Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "REQUEST_CANCELLED", "REQUEST",
                request.getPublicId(), null));
        publishAfterCommit(new RequestStatusChanged(request.getPublicId(), previous, request.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RequestSummary> findByPublicId(UUID publicId) {
        return store.findByPublicId(publicId).map(RequestService::summary);
    }

    private RequestEntity owned(AuthenticatedUser actor, UUID publicId) {
        CustomerAccountSummary owner = requireOwner(actor);
        RequestEntity request = store.findByPublicId(publicId).orElseThrow(RequestNotFoundException::new);
        if (!owner.publicId().equals(request.getOwnerAccountId())
                || !actor.userId().equals(request.getOwnerUserId())) {
            throw new RequestNotFoundException();
        }
        return request;
    }

    private CustomerAccountSummary requireOwner(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.CLIENT) {
            throw new AccessDeniedException("Client session required");
        }
        return customers.findOwnedBy(actor.userId()).orElseThrow(
                () -> new AccessDeniedException("Active owner account required"));
    }

    private void validate(CreateRequestCommand command) {
        if (command == null || command.type() == null || command.category() == null
                || !store.categoryExists(command.category())) {
            throw new IllegalArgumentException("Valid category is required");
        }
        if (command.title() == null || command.title().isBlank() || command.title().length() > 160
                || command.description() == null || command.description().isBlank()
                || command.description().length() > 10000) {
            throw new IllegalArgumentException("Title and description are invalid");
        }
        if (command.type() == RequestType.CUSTOM_DEVELOPMENT) {
            if (command.relatedModule() != null || command.impact() != null) {
                throw new IllegalArgumentException("Module fields do not apply to custom development");
            }
        } else if (command.impact() == null || command.impact().isBlank()
                || command.relatedModule() == null
                || !command.relatedModule().matches(MODULE_ALLOWED)) {
            throw new IllegalArgumentException("Module and impact are required");
        }
        if (command.estimatedBudget() != null && command.estimatedBudget().signum() < 0) {
            throw new IllegalArgumentException("Estimated budget cannot be negative");
        }
    }

    private RequestDetail detail(RequestEntity request) {
        return new RequestDetail(summary(request), request.getDescription(), request.getObjective(),
                request.getImpact(), request.getRelatedModule(), request.getDecisionReason(),
                request.getDuplicateReference(), store.history(request.getId()),
                store.attachments(request.getId()).stream().map(RequestAttachmentEntity::getFilePublicId).toList());
    }

    private static RequestSummary summary(RequestEntity request) {
        return new RequestSummary(request.getPublicId(), request.getReferenceCode(), request.getType(),
                request.getCategory(), request.getTitle(), request.getStatus(), request.getEstimatedBudget(),
                request.getTargetDate(), request.getCreatedAt());
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }
}
