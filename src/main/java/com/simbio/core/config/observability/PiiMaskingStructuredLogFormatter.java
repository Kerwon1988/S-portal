package com.simbio.core.config.observability;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

public class PiiMaskingStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    @Override
    public String format(ILoggingEvent event) {
        StringBuilder json = new StringBuilder(192)
                .append('{')
                .append("\"timestamp\":\"").append(escape(Instant.ofEpochMilli(event.getTimeStamp()).toString()))
                .append("\",\"level\":\"").append(escape(event.getLevel().toString()))
                .append("\",\"thread\":\"").append(escape(event.getThreadName()))
                .append("\",\"logger\":\"").append(escape(event.getLoggerName()))
                .append("\",\"message\":\"").append(escape(PiiMasker.mask(event.getFormattedMessage())))
                .append('"');

        Map<String, String> mdc = event.getMDCPropertyMap();
        String correlationId = mdc.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            json.append(",\"correlationId\":\"").append(escape(correlationId)).append('"');
        }
        if (event.getThrowableProxy() != null) {
            json.append(",\"exception\":\"").append(escape(event.getThrowableProxy().getClassName()))
                    .append("\",\"stack_trace\":\"***\"");
        }
        return json.append('}').toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
