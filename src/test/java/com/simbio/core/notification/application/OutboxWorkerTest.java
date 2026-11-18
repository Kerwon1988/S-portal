package com.simbio.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.domain.OutboxMessage;
import com.simbio.core.shared.value.EmailAddress;
import com.simbio.core.shared.spi.RecipientDirectory;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OutboxWorkerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final EmailCommand COMMAND = EmailCommand.toAddress(
            EmailAddress.of("synthetic@example.test"),
            EmailTemplate.EMAIL_VERIFICATION,
            Locale.ENGLISH,
            Map.of("url", "https://portal.example.test/verify?token=synthetic"));

    @Test
    void retriesTemporaryMailFailureWithBoundedBackoff() {
        Fixture fixture = fixture(3, COMMAND);
        doThrow(new MailSendException("synthetic transport failure"))
                .when(fixture.delivery).deliver(fixture.message.getId(), "synthetic@example.test", COMMAND);

        fixture.worker.processNext();

        assertThat(fixture.message.getStatus()).isEqualTo("PENDING");
        assertThat(fixture.message.getAttemptCount()).isEqualTo(1);
        assertThat(fixture.message.getAvailableAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(fixture.message.getFailureCode()).isEqualTo("MailSendException");
        verify(fixture.store).save(fixture.message);
    }

    @Test
    void marksDeliveryFailedAfterFinalAttempt() {
        Fixture fixture = fixture(1, COMMAND);
        doThrow(new MailSendException("synthetic transport failure"))
                .when(fixture.delivery).deliver(fixture.message.getId(), "synthetic@example.test", COMMAND);

        fixture.worker.processNext();

        assertThat(fixture.message.getStatus()).isEqualTo("FAILED");
        assertThat(fixture.message.getFailureCode()).isEqualTo("MailSendException");
        verify(fixture.store).save(fixture.message);
    }

    @Test
    void failsPermanentlyWhenUserRecipientCannotBeResolved() {
        UUID userId = UUID.randomUUID();
        EmailCommand userCommand = EmailCommand.toUser(
                userId, EmailTemplate.PASSWORD_RECOVERY, Locale.ENGLISH, Map.of());
        Fixture fixture = fixture(3, userCommand);
        when(fixture.recipients.emailAddressOf(userId)).thenReturn(Optional.empty());

        fixture.worker.processNext();

        assertThat(fixture.message.getStatus()).isEqualTo("FAILED");
        assertThat(fixture.message.getFailureCode()).isEqualTo("RecipientUnavailable");
        verifyNoInteractions(fixture.delivery);
        verify(fixture.store).save(fixture.message);
    }

    @Test
    void purgesOnlyAccordingToConfiguredRetentionAndReportsCount() {
        Fixture fixture = fixture(3, COMMAND);
        fixture.properties.setRetention(Duration.ofDays(30));
        when(fixture.store.purgePayloadsBefore(NOW.minus(Duration.ofDays(30)))).thenReturn(2);

        fixture.worker.purgeExpiredPayloads();

        verify(fixture.store).purgePayloadsBefore(NOW.minus(Duration.ofDays(30)));
        assertThat(fixture.meterRegistry.counter("portal.notification.outbox.payload.purged").count())
                .isEqualTo(2);
    }

    @Test
    void refusesToPurgeWithoutConfiguredRetention() {
        Fixture fixture = fixture(3, COMMAND);

        assertThatThrownBy(fixture.worker::purgeExpiredPayloads)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox retention must be configured before purging");
    }

    private static Fixture fixture(int maximumAttempts, EmailCommand command) {
        OutboxStore store = mock(OutboxStore.class);
        OutboxService outboxService = mock(OutboxService.class);
        EmailDeliveryPort delivery = mock(EmailDeliveryPort.class);
        NotificationProperties properties = new NotificationProperties();
        properties.setMaximumAttempts(maximumAttempts);
        properties.setInitialBackoff(Duration.ofSeconds(5));
        properties.setMaximumBackoff(Duration.ofSeconds(20));
        properties.setClaimLease(Duration.ofMinutes(2));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RecipientDirectory recipients = mock(RecipientDirectory.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        UUID messageId = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(messageId, "protected-payload", NOW);
        message.claimUntil(NOW.plus(properties.getClaimLease()));
        when(store.claimNext(NOW, NOW.plus(properties.getClaimLease()))).thenReturn(Optional.of(message));
        when(outboxService.decrypt(message)).thenReturn(command);

        OutboxWorker worker = new OutboxWorker(
                store, outboxService, delivery, properties, meterRegistry, recipients, clock);
        return new Fixture(store, delivery, properties, meterRegistry, recipients, message, worker);
    }

    private record Fixture(
            OutboxStore store,
            EmailDeliveryPort delivery,
            NotificationProperties properties,
            SimpleMeterRegistry meterRegistry,
            RecipientDirectory recipients,
            OutboxMessage message,
            OutboxWorker worker) {
    }
}
