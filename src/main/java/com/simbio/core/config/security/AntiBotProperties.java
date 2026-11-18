package com.simbio.core.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.security.anti-bot")
public class AntiBotProperties {

    private int proofDifficulty = 16;
    private int ttlSeconds = 120;
    private int maximumChallenges = 10_000;

    public int getProofDifficulty() {
        return proofDifficulty;
    }

    public void setProofDifficulty(int proofDifficulty) {
        if (proofDifficulty < 8 || proofDifficulty > 22) {
            throw new IllegalArgumentException("Anti-bot proof difficulty must be between 8 and 22 bits");
        }
        this.proofDifficulty = proofDifficulty;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        if (ttlSeconds < 30 || ttlSeconds > 600) {
            throw new IllegalArgumentException("Anti-bot challenge lifetime must be between 30 and 600 seconds");
        }
        this.ttlSeconds = ttlSeconds;
    }

    public int getMaximumChallenges() {
        return maximumChallenges;
    }

    public void setMaximumChallenges(int maximumChallenges) {
        if (maximumChallenges < 1) {
            throw new IllegalArgumentException("Maximum anti-bot challenge count must be positive");
        }
        this.maximumChallenges = maximumChallenges;
    }
}
