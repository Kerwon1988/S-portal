package com.simbio.core.request.application;

import java.time.Clock;
import java.time.Instant;
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
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestStatusChanged;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class RequestTransitionService {
    private final RequestStore store;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public RequestTransitionService(RequestStore store, AuditRecorder audit, ApplicationEventPublisher events) {
        this(store, audit, events, Clock.systemUTC());
    }

    RequestTransitionService(RequestStore store, AuditRecorder audit, ApplicationEventPublisher events, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public void transition(AuthenticatedUser actor, UUID requestPublicId, RequestStatus target,
            String reason, String originalReference) {
        requireStaff(actor);
        RequestEntity request = store.findByPublicId(requestPublicId).orElseThrow(RequestNotFoundException::new);
        if (target == RequestStatus.DUPLICADA) {
            store.findByReference(originalReference)
                    .orElseThrow(() -> new IllegalArgumentException("Original request does not exist"));
        }
        RequestStatus previous = request.getStatus();
        request.transition(target, reason, originalReference);
        store.save(request);
        store.saveHistory(new RequestStatusHistoryEntity(
                request.getId(), previous, target, actor.userId(), reason, Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "REQUEST_STATUS_CHANGED", "REQUEST",
                request.getPublicId(), previous + ">" + target));
        publishAfterCommit(new RequestStatusChanged(request.getPublicId(), previous, target));
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

    private static void requireStaff(AuthenticatedUser actor) {
        if (actor == null || actor.role() == Role.CLIENT
                || (actor.role() != Role.ADMIN && !actor.hasPermission(Permission.REQUESTS_VIEW_ALL))) {
            throw new AccessDeniedException("Request view permission required");
        }
    }
}
