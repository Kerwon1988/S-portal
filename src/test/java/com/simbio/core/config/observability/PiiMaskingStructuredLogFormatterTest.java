package com.simbio.core.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;

import org.junit.jupiter.api.Test;

class PiiMaskingStructuredLogFormatterTest {

    @Test
    void emitsStructuredJsonWithMaskedPiiAndCorrelationId() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setTimeStamp(Instant.parse("2026-11-11T00:00:00Z").toEpochMilli());
        event.setThreadName("test-thread");
        event.setLoggerName("com.simbio.test");
        event.setMessage("user=ana@example.com token=abc123");
        event.setMDCPropertyMap(Map.of(CorrelationIdFilter.MDC_KEY, "request-1234"));

        String json = new PiiMaskingStructuredLogFormatter().format(event);

        assertThat(json)
                .startsWith("{")
                .contains("\"message\":\"user=***@example.com token=***\"")
                .contains("\"correlationId\":\"request-1234\"")
                .doesNotContain("ana@", "abc123");
    }
}
