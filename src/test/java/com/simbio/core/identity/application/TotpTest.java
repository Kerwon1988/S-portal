package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpTest {

    @Test
    void matchesRfc6238SixDigitHmacSha1VectorAndPreventsReplay() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertThat(Totp.matchingStep(secret, "287082", 1, -1)).isEqualTo(1);
        assertThat(Totp.matchingStep(secret, "287082", 1, 1)).isEqualTo(-1);
        assertThat(Totp.matchingStep(secret, "28708", 1, -1)).isEqualTo(-1);
    }
}
