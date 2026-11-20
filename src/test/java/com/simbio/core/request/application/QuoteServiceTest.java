package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.customer.api.CustomerTaxIdentifierManager;
import com.simbio.core.request.api.QuoteDecision;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class QuoteServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private final RequestStore requests = org.mockito.Mockito.mock(RequestStore.class);
    private final QuoteStore quotes = org.mockito.Mockito.mock(QuoteStore.class);
    private final CustomerDirectory customers = org.mockito.Mockito.mock(CustomerDirectory.class);
    private final CustomerTaxIdentifierManager tax = org.mockito.Mockito.mock(CustomerTaxIdentifierManager.class);
    private final AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final QuoteService service = new QuoteService(requests, quotes, customers, tax, audit,
            events, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach void locale() { LocaleContextHolder.setLocale(java.util.Locale.ENGLISH); }
    @AfterEach void cleanup() { LocaleContextHolder.resetLocaleContext(); }

    @Test
    void employeeWithoutQuotePermissionCannotCreateDraft() {
        var actor = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.REQUESTS_VIEW_ALL));

        assertThatThrownBy(() -> service.createVersion(actor, UUID.randomUUID(), BigDecimal.TEN,
                "EUR", BigDecimal.ZERO, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(AccessDeniedException.class);
        verify(quotes, never()).save(any());
    }

    @Test
    void authorizedManagerSendsVersionAndEnqueuesNotification() {
        var actor = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.REQUESTS_VIEW_ALL, Permission.QUOTES_MANAGE));
        RequestEntity request = request(UUID.randomUUID(), UUID.randomUUID());
        request.transition(com.simbio.core.request.api.RequestStatus.EN_ANALISIS, null, null);
        QuoteEntity quote = quote(request);
        QuoteVersionEntity version = new QuoteVersionEntity(quote.getId(), 1, BigDecimal.TEN, "EUR",
                new BigDecimal("21"), LocalDate.of(2026, 12, 1), actor.userId(), NOW);
        when(requests.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));
        when(quotes.findByRequestId(request.getId())).thenReturn(Optional.of(quote));
        when(quotes.findVersion(quote.getId(), 1)).thenReturn(Optional.of(version));
        TransactionSynchronizationManager.initSynchronization();

        try {
            service.send(actor, request.getPublicId(), 1);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(events).publishEvent(any(com.simbio.core.request.api.QuoteSent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void foreignOwnerCannotDecideQuote() {
        var actor = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        var account = new CustomerAccountSummary(UUID.randomUUID(), "COMPANY", "Synthetic", "ACTIVE", NOW);
        RequestEntity request = request(UUID.randomUUID(), UUID.randomUUID());
        when(customers.findOwnedBy(actor.userId())).thenReturn(Optional.of(account));
        when(requests.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.decide(actor, request.getPublicId(), 1, QuoteDecision.ACCEPTED))
                .isInstanceOf(com.simbio.core.request.domain.RequestNotFoundException.class);
        verify(quotes, never()).saveVersion(any());
    }

    @Test
    void acceptedQuoteCannotBeReplayed() {
        var actor = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        var account = new CustomerAccountSummary(UUID.randomUUID(), "COMPANY", "Synthetic", "ACTIVE", NOW);
        RequestEntity request = request(account.publicId(), actor.userId());
        quoteState(request, QuoteDecision.ACCEPTED);
        assertThatThrownBy(() -> service.decide(actor, request.getPublicId(), 1, QuoteDecision.REJECTED))
                .isInstanceOf(IllegalStateException.class);
    }

    private void quoteState(RequestEntity request, QuoteDecision decision) {
        QuoteEntity quote = quote(request);
        QuoteVersionEntity version = new QuoteVersionEntity(quote.getId(), 1, BigDecimal.TEN, "EUR",
                BigDecimal.ZERO, LocalDate.of(2026, 12, 1), UUID.randomUUID(), NOW);
        version.send(NOW);
        version.decide(decision == QuoteDecision.ACCEPTED
                ? com.simbio.core.request.api.QuoteStatus.ACCEPTED
                : com.simbio.core.request.api.QuoteStatus.REJECTED, UUID.randomUUID(), NOW);
        when(customers.findOwnedBy(request.getOwnerUserId())).thenReturn(Optional.of(new CustomerAccountSummary(
                request.getOwnerAccountId(), "COMPANY", "Synthetic", "ACTIVE", NOW)));
        when(tax.maskedForOwnedAccount(any(), any())).thenReturn(Optional.of("A***1"));
        when(requests.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));
        when(quotes.findByRequestId(request.getId())).thenReturn(Optional.of(quote));
        when(quotes.findVersion(quote.getId(), 1)).thenReturn(Optional.of(version));
    }

    private static RequestEntity request(UUID account, UUID user) {
        var value = new RequestEntity(UUID.randomUUID(), "SOL-2026-000001", RequestType.CUSTOM_DEVELOPMENT,
                "WEB_APPLICATION", "Synthetic", "Synthetic", null, null, null, null, null,
                account, user, NOW);
        setId(value, 7L);
        return value;
    }

    private static QuoteEntity quote(RequestEntity request) {
        QuoteEntity quote = new QuoteEntity(UUID.randomUUID(), request.getId(), NOW);
        setId(quote, 9L);
        return quote;
    }

    private static void setId(Object object, long id) {
        try { var field = object.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(object, id); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
