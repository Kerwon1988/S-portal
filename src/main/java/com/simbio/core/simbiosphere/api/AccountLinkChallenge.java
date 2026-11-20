package com.simbio.core.simbiosphere.api;

import java.time.Instant;

public record AccountLinkChallenge(String challengeId, Instant expiresAt, int maximumAttempts) {
}
