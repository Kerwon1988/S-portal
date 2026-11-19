package com.simbio.core.config.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.simbio.core.shared.spi.AntiBotChallenge;
import com.simbio.core.shared.spi.AntiBotVerification;
import com.simbio.core.config.crypto.AltchaSigner;

@Component
public class AntiBotVerifier implements AntiBotVerification {

    private final ConcurrentHashMap<String, Instant> consumedChallenges = new ConcurrentHashMap<>();
    private final AntiBotProperties properties;
    private final AltchaSigner signer;
    private final MeterRegistry meterRegistry;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Autowired
    public AntiBotVerifier(
            AntiBotProperties properties,
            AltchaSigner signer,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this(properties, signer, meterRegistry, objectMapper, Clock.systemUTC());
    }

    AntiBotVerifier(
            AntiBotProperties properties,
            AltchaSigner signer,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.signer = signer;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AntiBotChallenge issueChallenge() {
        Instant now = Instant.now(clock);
        purgeConsumed(now);
        if (consumedChallenges.size() >= properties.getMaximumChallenges()) {
            meterRegistry.counter("portal.security.antibot.challenge", "outcome", "capacity").increment();
            throw new IllegalStateException("Anti-bot challenge capacity is exhausted");
        }
        Instant expires = now.plusSeconds(properties.getTtlSeconds());
        byte[] random = new byte[24];
        secureRandom.nextBytes(random);
        String salt = HexFormat.of().formatHex(random) + "|" + expires.getEpochSecond();
        int maxNumber = 1 << properties.getProofDifficulty();
        int target = secureRandom.nextInt(maxNumber + 1);
        String challenge = sha256(salt + target);
        return new AntiBotChallenge("SHA-256", challenge, maxNumber, salt, signer.sign(challenge), expires);
    }

    @Override
    public boolean verify(String altchaPayload, String honeypot, boolean accessibleConfirmation) {
        if (honeypot != null && !honeypot.isBlank()) {
            rejected();
            return false;
        }
        if (altchaPayload == null || altchaPayload.length() > 4096) {
            rejected();
            return false;
        }
        AltchaPayload payload;
        try {
            payload = objectMapper.readValue(Base64.getDecoder().decode(altchaPayload), AltchaPayload.class);
        } catch (IllegalArgumentException | JacksonException exception) {
            rejected();
            return false;
        }
        if (payload == null || !"SHA-256".equals(payload.algorithm())
                || payload.challenge() == null || !payload.challenge().matches("[0-9a-f]{64}")
                || payload.salt() == null || !payload.salt().matches("[0-9a-f]{48}\\|[0-9]{1,12}")
                || !signer.verifies(payload.challenge(), payload.signature())) {
            rejected();
            return false;
        }
        long expiry;
        try {
            expiry = Long.parseLong(payload.salt().substring(payload.salt().indexOf('|') + 1));
        } catch (NumberFormatException exception) {
            rejected();
            return false;
        }
        Instant now = Instant.now(clock);
        int maxNumber = 1 << properties.getProofDifficulty();
        boolean validProof = payload.number() != null
                && payload.number() >= 0
                && payload.number() <= maxNumber
                && challengeMatches(payload.salt(), payload.number(), payload.challenge());
        boolean accessibleFallback = accessibleConfirmation
                && payload.number() != null
                && payload.number() == -1;
        if (expiry <= now.getEpochSecond() || (!validProof && !accessibleFallback)) {
            rejected();
            return false;
        }
        purgeConsumed(now);
        String challengeId = digest(payload.challenge());
        if (consumedChallenges.size() >= properties.getMaximumChallenges()
                || consumedChallenges.putIfAbsent(challengeId, Instant.ofEpochSecond(expiry)) != null) {
            rejected();
            return false;
        }
        meterRegistry.counter("portal.security.antibot.challenge", "outcome", "accepted").increment();
        return true;
    }

    private static boolean challengeMatches(String salt, long number, String challenge) {
        return MessageDigest.isEqual(
                sha256(salt + number).getBytes(StandardCharsets.US_ASCII),
                challenge.getBytes(StandardCharsets.US_ASCII));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }

    private void purgeConsumed(Instant now) {
        consumedChallenges.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private void rejected() {
        meterRegistry.counter("portal.security.antibot.challenge", "outcome", "rejected").increment();
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }

    private record AltchaPayload(String algorithm, String challenge, Long number, String salt, String signature) {
    }
}
