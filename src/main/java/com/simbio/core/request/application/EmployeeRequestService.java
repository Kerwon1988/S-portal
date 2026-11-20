package com.simbio.core.request.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationView;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.RequestAssigned;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestStatusChanged;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestAssignmentEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.api.StaffDirectory;

@Service
public class EmployeeRequestService {
    private final RequestStore store;
    private final StaffDirectory staff;
    private final ConversationApi conversations;
    private final FileStorage files;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final int slaBusinessDays;
    private final Clock clock;

    @Autowired
    public EmployeeRequestService(RequestStore store, StaffDirectory staff, ConversationApi conversations,
            FileStorage files, AuditRecorder audit, ApplicationEventPublisher events,
            @Value("${portal.request.sla.first-response-business-days:1}") int slaBusinessDays) {
        this(store, staff, conversations, files, audit, events, slaBusinessDays, Clock.systemUTC());
    }

    EmployeeRequestService(RequestStore store, StaffDirectory staff, ConversationApi conversations,
            FileStorage files, AuditRecorder audit, ApplicationEventPublisher events, int slaBusinessDays, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.staff = Objects.requireNonNull(staff);
        this.conversations = Objects.requireNonNull(conversations);
        this.files = Objects.requireNonNull(files);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        if (slaBusinessDays < 1 || slaBusinessDays > 30) throw new IllegalArgumentException("SLA must be 1 to 30 business days");
        this.slaBusinessDays = slaBusinessDays;
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public RequestDashboard dashboard(AuthenticatedUser actor) {
        requireView(actor);
        return new RequestDashboard(store.countUnassigned(), store.countAssignedTo(actor.userId()),
                store.openForStaff(actor.userId(), 100).stream().filter(this::isOverdue).count(),
                store.countStatus(RequestStatus.EN_ESTUDIO), -1);
    }

    @Transactional(readOnly = true)
    public PageResult<EmployeeRequestSummary> search(AuthenticatedUser actor, RequestType type,
            RequestStatus status, String category, UUID assignedTo, Instant createdFrom,
            Instant createdBefore, BigDecimal amountFrom, BigDecimal amountTo, String search, PageQuery page) {
        requireView(actor);
        var result = store.searchStaff(type, status, category, assignedTo, createdFrom, createdBefore,
                amountFrom, amountTo, search, PageRequest.of(page.page(), page.size(),
                        Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PageResult<>(result.map(this::employeeSummary).getContent(), page.page(), page.size(),
                result.getTotalElements());
    }

    @Transactional
    public List<EmployeeRequestSummary> export(AuthenticatedUser actor, RequestType type,
            RequestStatus status, String category, UUID assignedTo) {
        requireView(actor);
        var result = store.searchStaff(type, status, category, assignedTo, null, null, null, null,
                null, PageRequest.of(0, 100, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        audit.record(new AuditRecord(actor.userId(), "REQUEST_CSV_EXPORTED", "REQUEST_EXPORT",
                UUID.randomUUID(), Integer.toString(result.getNumberOfElements())));
        return result.map(this::employeeSummary).getContent();
    }

    @Transactional(readOnly = true)
    public RequestDetail detail(AuthenticatedUser actor, UUID requestPublicId) {
        requireView(actor);
        RequestEntity request = request(requestPublicId);
        return new RequestDetail(summary(request), request.getDescription(), request.getObjective(),
                request.getImpact(), request.getRelatedModule(), request.getDecisionReason(),
                request.getDuplicateReference(), store.history(request.getId()),
                store.attachments(request.getId()).stream()
                        .map(value -> value.getFilePublicId()).toList());
    }

    @Transactional(readOnly = true)
    public InputStream downloadAttachment(AuthenticatedUser actor, UUID requestPublicId, UUID filePublicId) {
        requireView(actor);
        RequestEntity request = request(requestPublicId);
        store.findAttachment(request.getId(), filePublicId).orElseThrow(RequestNotFoundException::new);
        return files.open(filePublicId, ignored -> true);
    }

    @Transactional(readOnly = true)
    public List<RequestAssignmentEntity> assignments(AuthenticatedUser actor, UUID requestPublicId) {
        requireView(actor);
        return store.assignments(request(requestPublicId).getId());
    }

    @Transactional
    public void assign(AuthenticatedUser actor, UUID requestPublicId, UUID responsible,
            List<UUID> collaborators) {
        requireAssign(actor);
        RequestEntity request = request(requestPublicId);
        if (responsible == null) throw new IllegalArgumentException("Responsible staff is required");
        LinkedHashSet<UUID> all = new LinkedHashSet<>();
        all.add(responsible);
        if (collaborators != null) all.addAll(collaborators);
        all.forEach(id -> {
            var active = staff.findByUserId(id)
                    .filter(value -> "ACTIVE".equalsIgnoreCase(value.status()))
                    .orElseThrow(() -> new IllegalArgumentException("Staff must be active"));
            if (!id.equals(active.userPublicId())) throw new IllegalArgumentException("Staff identity mismatch");
        });
        Instant now = Instant.now(clock);
        List<RequestAssignmentEntity> values = all.stream()
                .map(id -> new RequestAssignmentEntity(request.getId(), id, id.equals(responsible), actor.userId(), now))
                .toList();
        store.save(request);
        store.replaceAssignments(request.getId(), values);
        audit.record(new AuditRecord(actor.userId(), "REQUEST_ASSIGNED", "REQUEST", request.getPublicId(),
                responsible.toString()));
        publishAfterCommit(new RequestAssigned(request.getPublicId(), responsible,
                all.stream().filter(id -> !id.equals(responsible)).toList()));
    }

    @Transactional
    public void update(AuthenticatedUser actor, UUID requestPublicId, RequestStatus target,
            String priority, BigDecimal estimateHours, String reason, String originalReference) {
        requireView(actor);
        RequestEntity request = request(requestPublicId);
        if (actor.role() != Role.ADMIN && !store.isAssigned(request.getId(), actor.userId())) {
            throw new AccessDeniedException("Assigned employee or administrator required");
        }
        RequestStatus previous = request.getStatus();
        if (target != null && target != previous) request.transition(target, reason, originalReference);
        request.updateOperational(priority, estimateHours, Instant.now(clock), false);
        store.save(request);
        if (target != null && target != previous) {
            store.saveHistory(new RequestStatusHistoryEntity(request.getId(), previous, target,
                    actor.userId(), reason, Instant.now(clock)));
            publishAfterCommit(new RequestStatusChanged(request.getPublicId(), previous, target));
        }
        audit.record(new AuditRecord(actor.userId(), "REQUEST_OPERATION_UPDATED", "REQUEST",
                request.getPublicId(), null));
    }

    @Transactional
    public void postMessage(AuthenticatedUser actor, UUID requestPublicId, MessageVisibility visibility,
            String body) {
        requireView(actor);
        RequestEntity request = request(requestPublicId);
        if (visibility == MessageVisibility.INTERNAL && !actor.hasPermission(Permission.INBOX_MANAGE)
                && actor.role() != Role.ADMIN) {
            throw new AccessDeniedException("Internal note permission required");
        }
        ConversationView conversation = conversations.findRequestForActor(actor, requestPublicId)
                .orElseThrow(RequestNotFoundException::new);
        conversations.postMessage(actor, conversation.publicId(), visibility, body);
        if (visibility == MessageVisibility.PUBLIC) {
            request.updateOperational(request.getPriority(), request.getEstimateHours(), Instant.now(clock), true);
            store.save(request);
        }
    }

    @Transactional(readOnly = true)
    public ConversationView conversation(AuthenticatedUser actor, UUID requestPublicId) {
        requireView(actor);
        request(requestPublicId);
        return conversations.findRequestForActor(actor, requestPublicId).orElseThrow(RequestNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<StaffWorkload> workload(AuthenticatedUser actor, String staffFilter) {
        requireView(actor);
        PageResult<com.simbio.core.staff.api.StaffSummary> directory = staff.search(staffFilter, "ACTIVE",
                PageQuery.of(0, 100));
        return directory.content().stream().map(person -> {
            List<RequestEntity> open = store.openForStaff(person.userPublicId(), 100);
            return new StaffWorkload(person.userPublicId(), person.jobTitle(), open.size(),
                    open.stream().filter(this::isOverdue).count());
        }).toList();
    }

    public boolean isOverdue(RequestEntity request) {
        if (request.getFirstResponseAt() != null) return false;
        return Instant.now(clock).isAfter(deadline(request).atTime(23, 59, 59).toInstant(ZoneOffset.UTC));
    }

    public LocalDate deadline(RequestEntity request) {
        return addBusinessDays(request.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), slaBusinessDays);
    }

    private EmployeeRequestSummary employeeSummary(RequestEntity request) {
        UUID responsible = store.assignments(request.getId()).stream()
                .filter(RequestAssignmentEntity::isResponsible).map(RequestAssignmentEntity::getStaffUserId)
                .findFirst().orElse(null);
        return new EmployeeRequestSummary(request.getPublicId(), request.getReferenceCode(), request.getType(),
                request.getCategory(), request.getTitle(), request.getStatus(), request.getPriority(),
                request.getEstimateHours(), request.getOwnerAccountId(), request.getOwnerUserId(),
                request.getCreatedAt(), responsible, isOverdue(request));
    }

    private static com.simbio.core.request.api.RequestSummary summary(RequestEntity request) {
        return new com.simbio.core.request.api.RequestSummary(request.getPublicId(), request.getReferenceCode(),
                request.getType(), request.getCategory(), request.getTitle(), request.getStatus(),
                request.getEstimatedBudget(), request.getTargetDate(), request.getCreatedAt());
    }

    private RequestEntity request(UUID requestPublicId) {
        return store.findByPublicId(requestPublicId).orElseThrow(RequestNotFoundException::new);
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }

    static LocalDate addBusinessDays(LocalDate start, int days) {
        LocalDate value = Objects.requireNonNull(start);
        int remaining = days;
        while (remaining > 0) {
            value = value.plusDays(1);
            if (value.getDayOfWeek() != DayOfWeek.SATURDAY && value.getDayOfWeek() != DayOfWeek.SUNDAY) remaining--;
        }
        return value;
    }

    static void requireView(AuthenticatedUser actor) {
        if (actor == null || actor.role() == Role.CLIENT
                || (actor.role() != Role.ADMIN && !actor.hasPermission(Permission.REQUESTS_VIEW_ALL))) {
            throw new AccessDeniedException("Request view permission required");
        }
    }

    private static void requireAssign(AuthenticatedUser actor) {
        requireView(actor);
        if (actor.role() != Role.ADMIN && !actor.hasPermission(Permission.REQUESTS_ASSIGN)) {
            throw new AccessDeniedException("Request assignment permission required");
        }
    }
}
