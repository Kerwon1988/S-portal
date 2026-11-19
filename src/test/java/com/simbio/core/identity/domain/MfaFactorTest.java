package com.simbio.core.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MfaFactorTest {

    @Test
    void pendingSecretCanBeReplacedButEnabledSecretCannot() {
        Instant now = Instant.parse("2026-11-18T12:00:00Z");
        MfaFactor factor = new MfaFactor(UUID.randomUUID(), "old-encrypted-secret", now);

        factor.replacePendingSecret("new-encrypted-secret", now.plusSeconds(1));
        assertThat(factor.getSecretEncrypted()).isEqualTo("new-encrypted-secret");

        factor.enable("", 0, now);
        assertThatThrownBy(() -> factor.replacePendingSecret("replacement", now.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MFA is already enabled");
    }

    @Test
    void recoveryHashesAreConsumedOnceAndTotpStepsMustAdvance() {
        Instant now = Instant.parse("2026-11-18T12:00:00Z");
        String recoveryHash = "synthetic-recovery-hash";
        MfaFactor factor = new MfaFactor(UUID.randomUUID(), "encrypted-secret", now);

        assertThat(factor.consumeRecoveryHash(recoveryHash, now)).isFalse();
        assertThatThrownBy(() -> factor.replaceRecoveryHashes(recoveryHash, now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MFA is not enabled");
        factor.enable(recoveryHash + ",another-hash", 3, now);

        assertThat(factor.consumeRecoveryHash("not-a-match", now)).isFalse();
        assertThat(factor.consumeRecoveryHash(recoveryHash, now.plusSeconds(1))).isTrue();
        assertThat(factor.consumeRecoveryHash(recoveryHash, now.plusSeconds(2))).isFalse();
        assertThatThrownBy(() -> factor.acceptStep(3, now.plusSeconds(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TOTP step has already been used");
        factor.acceptStep(4, now.plusSeconds(4));
        factor.replaceRecoveryHashes("replacement-hash", now.plusSeconds(5));
        assertThat(factor.getLastAcceptedStep()).isEqualTo(4);
    }
}
