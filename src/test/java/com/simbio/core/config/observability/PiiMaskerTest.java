package com.simbio.core.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    @Test
    void masksEmailsTokensAndStackTraces() {
        String masked = PiiMasker.mask(
                "user=ana@example.com token=abc123 stack_trace=private-details");

        assertThat(masked)
                .contains("***@example.com")
                .contains("token=***")
                .contains("stack_trace=***")
                .doesNotContain("ana@", "abc123", "private-details");
    }
}
