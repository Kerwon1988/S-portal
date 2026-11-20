package com.simbio.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.application.OutboxStore;
import com.simbio.core.notification.domain.OutboxMessage;
import com.simbio.core.shared.value.EmailAddress;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;

@SpringBootTest(properties = "portal.notification.worker.enabled=false")
@ActiveProfiles("test")
class OutboxPostCommitH2IT {

    @MockitoBean
    private DemoProvisioningClient demoProvisioningClient;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Test
    void enqueuedMessageIsNotVisibleToWorkerBeforeOuterTransactionCommits() {
        long baseline = outboxStore.countByStatus("PENDING");
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        UUID messageId = outer.execute(status -> {
            UUID id = emailSender.enqueue(EmailCommand.toAddress(
                    EmailAddress.of("synthetic@example.test"),
                    EmailTemplate.EMAIL_VERIFICATION,
                    Locale.ENGLISH,
                    Map.of("url", "https://portal.example.test/verify?token=synthetic")));
            entityManager.flush();

            TransactionTemplate concurrentRead = new TransactionTemplate(transactionManager);
            concurrentRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            long visiblePending = concurrentRead.execute(innerStatus -> outboxStore.countByStatus("PENDING"));
            assertThat(visiblePending).isEqualTo(baseline);
            return id;
        });

        assertThat(messageId).isNotNull();
        try {
            assertThat(outboxStore.countByStatus("PENDING")).isEqualTo(baseline + 1);
        } finally {
            outboxStore.findById(messageId).ifPresent(message -> {
                message.markFailed("TestCleanup");
                outboxStore.save(message);
            });
        }
    }

    @Test
    void repeatedDomainEventUsesOneDeterministicOutboxId() {
        String key = "REQUEST_CREATED:" + UUID.randomUUID();
        long baseline = outboxStore.countByStatus("PENDING");
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"), EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH, Map.of("url", "https://portal.example.test/verify?token=synthetic"));

        UUID first = emailSender.enqueueOnce(key, command);
        UUID second = emailSender.enqueueOnce(key, command);

        assertThat(first).isEqualTo(second);
        assertThat(outboxStore.findById(first)).isPresent();
        assertThat(outboxStore.countByStatus("PENDING")).isEqualTo(baseline + 1);
    }

    @Test
    void purgesPayloadsOnlyForExpiredSentOrFailedMessages() {
        Instant now = Instant.now();
        Instant old = now.minus(Duration.ofDays(45));
        Instant cutoff = now.minus(Duration.ofDays(30));
        OutboxMessage sent = new OutboxMessage(UUID.randomUUID(), "sent-payload", old);
        sent.markSent(now);
        OutboxMessage failed = new OutboxMessage(UUID.randomUUID(), "failed-payload", old);
        failed.markFailed("SyntheticFailure");
        OutboxMessage recentSent = new OutboxMessage(UUID.randomUUID(), "recent-payload", now);
        recentSent.markSent(now);
        OutboxMessage pending = new OutboxMessage(UUID.randomUUID(), "pending-payload", old);
        outboxStore.save(sent);
        outboxStore.save(failed);
        outboxStore.save(recentSent);
        outboxStore.save(pending);

        assertThat(outboxStore.purgePayloadsBefore(cutoff)).isGreaterThanOrEqualTo(2);
        assertThat(outboxStore.findById(sent.getId()).orElseThrow().getPayloadEncrypted()).isNull();
        assertThat(outboxStore.findById(failed.getId()).orElseThrow().getPayloadEncrypted()).isNull();
        assertThat(outboxStore.findById(recentSent.getId()).orElseThrow().getPayloadEncrypted())
                .isEqualTo("recent-payload");
        assertThat(outboxStore.findById(pending.getId()).orElseThrow().getPayloadEncrypted())
                .isEqualTo("pending-payload");
    }
}
