package com.simbio.core.notification.application;

import java.time.Clock;
import java.time.Instant;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.simbio.core.notification.domain.OutboxMessage;
import com.simbio.core.shared.spi.RecipientDirectory;

@Component
@ConditionalOnProperty(prefix = "portal.notification.worker", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OutboxWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxWorker.class);
    private final OutboxStore store;
    private final OutboxService outboxService;
    private final EmailDeliveryPort emailSender;
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;
    private final RecipientDirectory recipients;
    private final DeliveryPolicy deliveryPolicy;
    private final Clock clock;

    @Autowired
    public OutboxWorker(
            OutboxStore store,
            OutboxService outboxService,
            EmailDeliveryPort emailSender,
            NotificationProperties properties,
            MeterRegistry meterRegistry,
            RecipientDirectory recipients) {
        this(store, outboxService, emailSender, properties, meterRegistry, recipients, Clock.systemUTC());
    }

    OutboxWorker(
            OutboxStore store,
            OutboxService outboxService,
            EmailDeliveryPort emailSender,
            NotificationProperties properties,
            MeterRegistry meterRegistry,
            RecipientDirectory recipients,
            Clock clock) {
        this.store = store;
        this.outboxService = outboxService;
        this.emailSender = emailSender;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.recipients = recipients;
        deliveryPolicy = new DeliveryPolicy(properties.getMaximumAttempts(),
                properties.getInitialBackoff(), properties.getMaximumBackoff());
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${portal.notification.poll-interval:5000}")
    public void processNext() {
        Instant now = Instant.now(clock);
        store.claimNext(now, now.plus(properties.getClaimLease())).ifPresent(this::deliver);
    }

    @Scheduled(cron = "${portal.notification.purge-cron:0 0 3 * * *}")
    public void purgeExpiredPayloads() {
        Instant cutoff = Instant.now(clock).minus(requireRetention());
        int purged = store.purgePayloadsBefore(cutoff);
        meterRegistry.counter("portal.notification.outbox.payload.purged").increment(purged);
    }

    private java.time.Duration requireRetention() {
        java.time.Duration retention = properties.getRetention();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalStateException("Outbox retention must be configured before purging");
        }
        return retention;
    }

    private void deliver(OutboxMessage message) {
        try {
            var command = outboxService.decrypt(message);
            String recipient = command.recipientUserId() == null
                    ? command.recipient().value()
                    : recipients.emailAddressOf(command.recipientUserId())
                            .map(com.simbio.core.shared.value.EmailAddress::value)
                            .orElse(null);
            if (recipient == null) {
                message.markFailed("RecipientUnavailable");
                store.save(message);
                meterRegistry.counter("portal.notification.delivery", "outcome", "failed").increment();
                return;
            }
            emailSender.deliver(message.getId(), recipient, command);
            message.markSent(Instant.now(clock));
            store.save(message);
            meterRegistry.counter("portal.notification.delivery", "outcome", "sent").increment();
        } catch (MailException exception) {
            recordFailure(message, exception);
        }
    }

    private void recordFailure(OutboxMessage message, MailException exception) {
        String failureCode = exception.getClass().getSimpleName();
        if (deliveryPolicy.hasAttemptsRemaining(message.getAttemptCount())) {
            message.scheduleRetry(Instant.now(clock).plus(deliveryPolicy.backoffAfter(message.getAttemptCount())),
                    failureCode);
            meterRegistry.counter("portal.notification.delivery", "outcome", "retry").increment();
        } else {
            message.markFailed(failureCode);
            meterRegistry.counter("portal.notification.delivery", "outcome", "failed").increment();
        }
        store.save(message);
        LOGGER.warn("Email delivery failed for outbox message {} ({})", message.getId(), failureCode);
    }
}
