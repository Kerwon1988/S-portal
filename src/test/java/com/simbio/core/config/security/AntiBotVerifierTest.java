package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.simbio.core.config.crypto.AltchaSigner;
import com.simbio.core.config.crypto.EncryptionProperties;
import com.simbio.core.shared.spi.AntiBotChallenge;

class AntiBotVerifierTest {

    @Test
    void acceptsOneValidProofAndRejectsReplayAndHoneypot() throws Exception {
        AntiBotProperties properties = properties();
        EncryptionProperties encryptionProperties = encryptionProperties();
        AntiBotVerifier verifier = new AntiBotVerifier(properties, new AltchaSigner(encryptionProperties),
                new SimpleMeterRegistry(), new ObjectMapper());
        AntiBotChallenge challenge = verifier.issueChallenge();
        long solution = findProof(challenge);
        String payload = payload(challenge, solution);

        assertThat(verifier.verify(payload, "", false)).isTrue();
        assertThat(verifier.verify(payload, "", false)).isFalse();
        AntiBotChallenge trapped = verifier.issueChallenge();
        assertThat(verifier.verify(trapped.accessiblePayload(), "bot-field", true)).isFalse();
        assertThat(verifier.verify(trapped.accessiblePayload(), "", true)).isTrue();
    }

    @Test
    void rejectsInvalidSignatureAndExpiredChallenge() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AntiBotVerifier verifier = verifier(clock);
        AntiBotChallenge challenge = verifier.issueChallenge();
        long solution = findProof(challenge);

        assertThat(verifier.verify(payload(challenge, solution, "0".repeat(64)), "", false)).isFalse();

        clock.set(challenge.expiresAt());
        assertThat(verifier.verify(payload(challenge, solution), "", false)).isFalse();
    }

    @Test
    void rejectsAccessibleFallbackWithoutExplicitConfirmation() {
        AntiBotVerifier verifier = verifier(new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));
        AntiBotChallenge challenge = verifier.issueChallenge();

        assertThat(verifier.verify(challenge.accessiblePayload(), "", false)).isFalse();
        assertThat(verifier.verify(challenge.accessiblePayload(), "", true)).isTrue();
    }

    @Test
    void consumesAValidProofOnlyOnceAcrossConcurrentSubmissions() throws Exception {
        AntiBotVerifier verifier = verifier(new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));
        AntiBotChallenge challenge = verifier.issueChallenge();
        String payload = payload(challenge, findProof(challenge));
        int parallelism = 8;
        ExecutorService workers = Executors.newFixedThreadPool(parallelism);
        CountDownLatch ready = new CountDownLatch(parallelism);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> attempts = new ArrayList<>(parallelism);

        try {
            for (int attempt = 0; attempt < parallelism; attempt++) {
                attempts.add(workers.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent anti-bot test did not start");
                    }
                    return verifier.verify(payload, "", false);
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            long accepted = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(1);
        } finally {
            workers.shutdownNow();
        }
    }

    private static AntiBotVerifier verifier(Clock clock) {
        return new AntiBotVerifier(properties(), new AltchaSigner(encryptionProperties()),
                new SimpleMeterRegistry(), new ObjectMapper(), clock);
    }

    private static AntiBotProperties properties() {
        AntiBotProperties properties = new AntiBotProperties();
        properties.setProofDifficulty(8);
        properties.setTtlSeconds(60);
        return properties;
    }

    private static EncryptionProperties encryptionProperties() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setCurrentKeyId("test-key");
        properties.setKeys("test-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return properties;
    }

    private static long findProof(AntiBotChallenge challenge) throws Exception {
        for (int attempt = 0; attempt <= challenge.maxNumber(); attempt++) {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((challenge.salt() + attempt).getBytes(StandardCharsets.UTF_8));
            if (HexFormat.of().formatHex(hash).equals(challenge.challenge())) {
                return attempt;
            }
        }
        throw new AssertionError("Could not find a bounded proof");
    }

    private static String payload(AntiBotChallenge challenge, long number) {
        return payload(challenge, number, challenge.signature());
    }

    private static String payload(AntiBotChallenge challenge, long number, String signature) {
        String json = "{\"algorithm\":\"" + challenge.algorithm() + "\",\"challenge\":\""
                + challenge.challenge() + "\",\"number\":" + number + ",\"salt\":\""
                + challenge.salt() + "\",\"signature\":\"" + signature + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant.get());
        }

        @Override
        public Instant instant() {
            return instant.get();
        }

        private void set(Instant value) {
            instant.set(value);
        }
    }
}
