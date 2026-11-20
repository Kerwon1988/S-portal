package com.simbio.core.request.application;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.api.RequestNotificationRequested;
import com.simbio.core.request.api.QuoteSent;
import com.simbio.core.request.api.RequestCreated;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestStatusChanged;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.shared.spi.LocalePreference;

class RequestNotificationListenerTest {

    @Test
    void translatesAllRequestEventsIntoNotificationOwnedContract() {
        RequestStore requests = mock(RequestStore.class);
        LocalePreference locales = mock(LocalePreference.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        RequestEntity request = request();
        when(requests.findByPublicId(request.getPublicId())).thenReturn(Optional.of(request));
        when(locales.findLocale(request.getOwnerUserId())).thenReturn(Optional.of("en"));
        RequestNotificationListener listener = new RequestNotificationListener(requests, locales, events);

        listener.on(new RequestCreated(request.getPublicId(), request.getOwnerAccountId()));
        listener.on(new RequestStatusChanged(request.getPublicId(), RequestStatus.RECIBIDA,
                RequestStatus.EN_ANALISIS));
        UUID quoteId = UUID.randomUUID();
        listener.on(new QuoteSent(request.getPublicId(), quoteId, 2));

        verify(events).publishEvent(org.mockito.ArgumentMatchers.<Object>argThat(value -> matches(value,
                "REQUEST_CREATED:" + request.getPublicId(), EmailTemplate.REQUEST_CREATED,
                request, Locale.ENGLISH)));
        verify(events).publishEvent(org.mockito.ArgumentMatchers.<Object>argThat(value -> matches(value,
                "REQUEST_STATUS:" + request.getPublicId() + ":EN_ANALISIS",
                EmailTemplate.REQUEST_STATUS_CHANGED, request, Locale.ENGLISH)));
        verify(events).publishEvent(org.mockito.ArgumentMatchers.<Object>argThat(value -> matches(value,
                "QUOTE_SENT:" + quoteId + ":2", EmailTemplate.QUOTE_SENT,
                request, Locale.ENGLISH)));
    }

    @Test
    void unknownRequestDoesNotGuessRecipient() {
        RequestStore requests = mock(RequestStore.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        UUID requestId = UUID.randomUUID();
        when(requests.findByPublicId(requestId)).thenReturn(Optional.empty());

        new RequestNotificationListener(requests, mock(LocalePreference.class), events)
                .on(new RequestCreated(requestId, UUID.randomUUID()));

        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private static boolean matches(Object value, String key, EmailTemplate template,
            RequestEntity request, Locale locale) {
        if (!(value instanceof RequestNotificationRequested event)) {
            return false;
        }
        return event.recipientUserId().equals(request.getOwnerUserId())
                && event.internalPath().equals("/cuenta/solicitudes/" + request.getPublicId())
                && event.template() == template
                && event.locale().equals(locale)
                && event.idempotencyKey().equals(key);
    }

    private static RequestEntity request() {
        return new RequestEntity(UUID.randomUUID(), "SOL-2026-000001",
                RequestType.CUSTOM_DEVELOPMENT, "WEB", "Synthetic", "Synthetic description",
                null, null, null, BigDecimal.ZERO, null, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    }
}
