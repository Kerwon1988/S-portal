package com.simbio.core.request.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.customer.api.CustomerTaxIdentifierManager;
import com.simbio.core.request.api.QuoteDecision;
import com.simbio.core.request.api.QuoteDecisionMade;
import com.simbio.core.request.api.QuoteSent;
import com.simbio.core.request.api.QuoteStatus;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class QuoteService {
    private final RequestStore requests;
    private final QuoteStore quotes;
    private final CustomerDirectory customers;
    private final CustomerTaxIdentifierManager taxIdentifiers;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public QuoteService(RequestStore requests, QuoteStore quotes, CustomerDirectory customers,
            CustomerTaxIdentifierManager taxIdentifiers, AuditRecorder audit,
            ApplicationEventPublisher events) {
        this(requests, quotes, customers, taxIdentifiers, audit, events, Clock.systemUTC());
    }

    QuoteService(RequestStore requests, QuoteStore quotes, CustomerDirectory customers,
            CustomerTaxIdentifierManager taxIdentifiers, AuditRecorder audit,
            ApplicationEventPublisher events, Clock clock) {
        this.requests = Objects.requireNonNull(requests);
        this.quotes = Objects.requireNonNull(quotes);
        this.customers = Objects.requireNonNull(customers);
        this.taxIdentifiers = Objects.requireNonNull(taxIdentifiers);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public QuoteVersionView createVersion(AuthenticatedUser actor, UUID requestPublicId,
            BigDecimal netAmount, String currency, BigDecimal taxRate, LocalDate validUntil) {
        requireQuoteManager(actor);
        RequestEntity request = request(requestPublicId);
        QuoteEntity quote = quotes.findByRequestId(request.getId())
                .orElseGet(() -> quotes.save(new QuoteEntity(UUID.randomUUID(), request.getId(), Instant.now(clock))));
        Optional<QuoteVersionEntity> latest = quotes.latest(quote.getId());
        if (latest.isPresent() && latest.get().getStatus() == QuoteStatus.DRAFT) {
            throw new RequestConflictException("A draft quote already exists");
        }
        if (latest.isPresent() && latest.get().getStatus() == QuoteStatus.SENT) {
            throw new RequestConflictException("The sent quote must expire or be decided before versioning");
        }
        if (latest.isPresent() && (latest.get().getStatus() == QuoteStatus.ACCEPTED
                || latest.get().getStatus() == QuoteStatus.REJECTED)) {
            throw new RequestConflictException("A decided quote cannot be edited");
        }
        QuoteVersionEntity version = quotes.saveVersion(new QuoteVersionEntity(quote.getId(),
                latest.map(v -> v.getVersionNumber() + 1).orElse(1), netAmount,
                currency == null ? null : currency.trim().toUpperCase(Locale.ROOT), taxRate,
                validUntil, actor.userId(), Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "QUOTE_VERSION_CREATED", "QUOTE",
                quote.getPublicId(), Integer.toString(version.getVersionNumber())));
        return view(quote, version);
    }

    @Transactional
    public QuoteVersionView send(AuthenticatedUser actor, UUID requestPublicId, int versionNumber) {
        requireQuoteManager(actor);
        RequestEntity request = request(requestPublicId);
        QuoteEntity quote = quotes.findByRequestId(request.getId()).orElseThrow(RequestNotFoundException::new);
        QuoteVersionEntity version = quotes.findVersion(quote.getId(), versionNumber)
                .orElseThrow(RequestNotFoundException::new);
        version.send(Instant.now(clock));
        quotes.saveVersion(version);
        var previous = request.getStatus();
        request.transition(com.simbio.core.request.api.RequestStatus.PRESUPUESTO_ENVIADO,
                "QUOTE_SENT", null);
        requests.save(request);
        requests.saveHistory(new com.simbio.core.request.domain.RequestStatusHistoryEntity(request.getId(),
                previous, request.getStatus(), actor.userId(), "QUOTE_SENT", Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "QUOTE_SENT", "QUOTE", quote.getPublicId(),
                Integer.toString(versionNumber)));
        publishAfterCommit(new QuoteSent(request.getPublicId(), quote.getPublicId(), versionNumber));
        return view(quote, version);
    }

    @Transactional(readOnly = true)
    public List<QuoteVersionView> forEmployee(AuthenticatedUser actor, UUID requestPublicId) {
        requireStaff(actor);
        RequestEntity request = request(requestPublicId);
        return quoteViews(request);
    }

    @Transactional(readOnly = true)
    public List<QuoteVersionView> forOwner(AuthenticatedUser actor, UUID requestPublicId) {
        RequestEntity request = ownedRequest(actor, requestPublicId);
        return quoteViews(request);
    }

    @Transactional
    public QuoteVersionView decide(AuthenticatedUser actor, UUID requestPublicId, int versionNumber,
            QuoteDecision decision) {
        RequestEntity request = ownedRequest(actor, requestPublicId);
        if (decision == null) throw new IllegalArgumentException("Quote decision is required");
        if (taxIdentifiers.maskedForOwnedAccount(actor, request.getOwnerAccountId()).isEmpty()) {
            throw new RequestConflictException("A valid tax identifier is required before deciding a quote");
        }
        QuoteEntity quote = quotes.findByRequestId(request.getId()).orElseThrow(RequestNotFoundException::new);
        QuoteVersionEntity version = quotes.findVersion(quote.getId(), versionNumber)
                .orElseThrow(RequestNotFoundException::new);
        version.decide(decision == QuoteDecision.ACCEPTED ? QuoteStatus.ACCEPTED : QuoteStatus.REJECTED,
                actor.userId(), Instant.now(clock));
        quotes.saveVersion(version);
        var previous = request.getStatus();
        request.transition(decision == QuoteDecision.ACCEPTED
                ? com.simbio.core.request.api.RequestStatus.ACEPTADA
                : com.simbio.core.request.api.RequestStatus.RECHAZADA, "QUOTE_DECISION_" + decision, null);
        requests.save(request);
        requests.saveHistory(new com.simbio.core.request.domain.RequestStatusHistoryEntity(request.getId(),
                previous, request.getStatus(), actor.userId(), "QUOTE_DECISION_" + decision, Instant.now(clock)));
        audit.record(new AuditRecord(actor.userId(), "QUOTE_" + decision, "QUOTE", quote.getPublicId(),
                Integer.toString(versionNumber)));
        publishAfterCommit(new QuoteDecisionMade(request.getPublicId(), quote.getPublicId(), versionNumber, decision));
        return view(quote, version);
    }

    private List<QuoteVersionView> quoteViews(RequestEntity request) {
        return quotes.findByRequestId(request.getId()).map(quote -> quotes.versions(quote.getId()).stream()
                .map(version -> view(quote, version)).toList()).orElseGet(List::of);
    }

    private RequestEntity ownedRequest(AuthenticatedUser actor, UUID publicId) {
        if (actor == null || actor.role() != Role.CLIENT) throw new AccessDeniedException("Client session required");
        var account = customers.findOwnedBy(actor.userId())
                .orElseThrow(() -> new AccessDeniedException("Active owner account required"));
        RequestEntity request = request(publicId);
        if (!account.publicId().equals(request.getOwnerAccountId())
                || !actor.userId().equals(request.getOwnerUserId())) throw new RequestNotFoundException();
        return request;
    }

    private RequestEntity request(UUID publicId) {
        return requests.findByPublicId(Objects.requireNonNull(publicId)).orElseThrow(RequestNotFoundException::new);
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

    private static QuoteVersionView view(QuoteEntity quote, QuoteVersionEntity version) {
        return new QuoteVersionView(quote.getPublicId(), version.getVersionNumber(), version.getNetAmount(),
                version.getCurrency(), version.getTaxRate(), version.getTaxAmount(), version.getGrossAmount(),
                version.getValidUntil(), version.getStatus(), version.getCreatedAt(), version.getSentAt(),
                version.getDecidedBy(), version.getDecidedAt());
    }

    static void requireStaff(AuthenticatedUser actor) {
        if (actor == null || actor.role() == Role.CLIENT
                || (actor.role() != Role.ADMIN && !actor.hasPermission(Permission.REQUESTS_VIEW_ALL))) {
            throw new AccessDeniedException("Request view permission required");
        }
    }

    private static void requireQuoteManager(AuthenticatedUser actor) {
        requireStaff(actor);
        if (actor.role() != Role.ADMIN && !actor.hasPermission(Permission.QUOTES_MANAGE)) {
            throw new AccessDeniedException("Quote management permission required");
        }
    }
}
