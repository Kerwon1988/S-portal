package com.simbio.core.shared.spi;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record AntiBotChallenge(
        String algorithm,
        String challenge,
        int maxNumber,
        String salt,
        String signature,
        Instant expiresAt) {

    public String accessiblePayload() {
        String json = "{\"algorithm\":\"" + algorithm + "\",\"challenge\":\"" + challenge
                + "\",\"number\":-1,\"salt\":\"" + salt + "\",\"signature\":\"" + signature + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "AntiBotChallenge[algorithm=" + algorithm + ", challenge=<redacted>, maxnumber="
                + maxNumber + ", salt=<redacted>, signature=<redacted>, expiresAt=" + expiresAt + "]";
    }
}
