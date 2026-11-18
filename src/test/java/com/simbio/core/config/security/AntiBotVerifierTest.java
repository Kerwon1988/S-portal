package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
        String json = "{\"algorithm\":\"" + challenge.algorithm() + "\",\"challenge\":\""
                + challenge.challenge() + "\",\"number\":" + number + ",\"salt\":\""
                + challenge.salt() + "\",\"signature\":\"" + challenge.signature() + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
