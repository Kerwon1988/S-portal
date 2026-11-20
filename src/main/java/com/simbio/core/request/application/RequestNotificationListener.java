package com.simbio.core.request.application;

import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.api.RequestNotificationRequested;
import com.simbio.core.request.api.QuoteSent;
import com.simbio.core.request.api.RequestCreated;
import com.simbio.core.request.api.RequestStatusChanged;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.shared.spi.LocalePreference;

@Component
public class RequestNotificationListener {

    private final RequestStore requests;
    private final LocalePreference locales;
    private final ApplicationEventPublisher events;

    public RequestNotificationListener(
            RequestStore requests,
            LocalePreference locales,
            ApplicationEventPublisher events) {
        this.requests = requests;
        this.locales = locales;
        this.events = events;
    }

    @EventListener
    public void on(RequestCreated event) {
        publish(event.requestPublicId(),
                "REQUEST_CREATED:" + event.requestPublicId(), EmailTemplate.REQUEST_CREATED);
    }

    @EventListener
    public void on(RequestStatusChanged event) {
        publish(event.requestPublicId(),
                "REQUEST_STATUS:" + event.requestPublicId() + ":" + event.current(),
                EmailTemplate.REQUEST_STATUS_CHANGED);
    }

    @EventListener
    public void on(QuoteSent event) {
        publish(event.requestPublicId(),
                "QUOTE_SENT:" + event.quotePublicId() + ":" + event.versionNumber(),
                EmailTemplate.QUOTE_SENT);
    }

    private void publish(java.util.UUID requestPublicId, String key, EmailTemplate template) {
        RequestEntity request = requests.findByPublicId(requestPublicId).orElse(null);
        if (request == null) {
            return;
        }
        String language = locales.findLocale(request.getOwnerUserId()).orElse("es");
        Locale locale = Locale.forLanguageTag("en".equals(language) ? "en" : "es");
        events.publishEvent((Object) new RequestNotificationRequested(
                request.getOwnerUserId(),
                "/cuenta/solicitudes/" + request.getPublicId(),
                template,
                locale,
                key));
    }
}
