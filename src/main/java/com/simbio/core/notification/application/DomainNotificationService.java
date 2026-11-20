package com.simbio.core.notification.application;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.messaging.api.MessageNotificationApi;
import com.simbio.core.messaging.api.MessagePosted;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.api.RequestNotificationRequested;
import com.simbio.core.shared.spi.LocalePreference;

@Service
public class DomainNotificationService {

    private final MessageNotificationApi messages;
    private final LocalePreference locales;
    private final EmailSender emails;

    public DomainNotificationService(
            MessageNotificationApi messages,
            LocalePreference locales,
            EmailSender emails) {
        this.messages = messages;
        this.locales = locales;
        this.emails = emails;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> on(MessagePosted event) {
        return messages.notificationTarget(event.conversationPublicId())
                .filter(target -> target.recipientUserId() != null && target.internalPath() != null)
                .map(target -> {
                    Locale locale = localeOf(target.recipientUserId(), target.locale());
                    return emails.enqueue(EmailCommand.toUser(
                            target.recipientUserId(), EmailTemplate.MESSAGE_POSTED, locale,
                            Map.of("url", target.internalPath())));
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> on(MessageReceived event) {
        return messages.notificationTarget(event.conversationPublicId())
                .filter(target -> target.recipientAddress() != null)
                .map(target -> emails.enqueue(EmailCommand.toAddress(
                        target.recipientAddress(), EmailTemplate.CONTACT_ACKNOWLEDGEMENT,
                        target.locale(), Map.of())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID on(RequestNotificationRequested event) {
        return emails.enqueueOnce(event.idempotencyKey(),
                EmailCommand.toUser(event.recipientUserId(), event.template(),
                        localeOf(event.recipientUserId(), event.locale()),
                        Map.of("url", event.internalPath())));
    }

    private Locale localeOf(UUID recipientUserId, Locale fallback) {
        String language = locales.findLocale(recipientUserId).orElse(fallback.getLanguage());
        return Locale.forLanguageTag("en".equals(language) ? "en" : "es");
    }
}
