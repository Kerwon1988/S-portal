package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.identity.application.OneTimeTokenService;
import com.simbio.core.identity.domain.TokenPurpose;

@SpringBootTest
@ActiveProfiles("test")
class OneTimeTokenH2IT {

    @Autowired
    private OneTimeTokenService tokenService;

    @Test
    void tokenIsConsumedExactlyOnceWhenRequestsRace() throws Exception {
        String token = tokenService.issueForUser(UUID.randomUUID(), TokenPurpose.PASSWORD_RESET,
                Duration.ofMinutes(5));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger consumed = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int index = 0; index < 2; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start");
                    }
                    if (tokenService.consume(token, TokenPurpose.PASSWORD_RESET).isPresent()) {
                        consumed.incrementAndGet();
                    }
                    return null;
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
            assertThat(consumed.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invitationCorrelationSurvivesPersistenceAndConsumption() {
        UUID reference = UUID.randomUUID();
        String token = tokenService.issueInvitation("synthetic@example.test", "CLIENT", reference,
                Duration.ofMinutes(5));

        var consumed = tokenService.consume(token, TokenPurpose.INVITATION);

        assertThat(consumed).isPresent();
        assertThat(consumed.orElseThrow().invitationReference()).isEqualTo(reference);
        assertThat(consumed.orElseThrow().toString()).doesNotContain("synthetic@example.test");
    }
}
