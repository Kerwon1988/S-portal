package com.simbio.core.config.observability;

import java.util.Objects;
import java.util.regex.Pattern;

public final class PiiMasker {

    private static final Pattern EMAIL =
            Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@([A-Z0-9.-]+\\.[A-Z]{2,})\\b");
    private static final Pattern TOKEN =
            Pattern.compile("(?i)\\b(token|access_token|authorization)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern STACK_TRACE =
            Pattern.compile("(?i)\\bstack_trace\\s*[:=]\\s*[^\\r\\n,;]*");

    private PiiMasker() {
    }

    public static String mask(String message) {
        Objects.requireNonNull(message, "message must not be null");
        String masked = EMAIL.matcher(message).replaceAll("***@$1");
        masked = TOKEN.matcher(masked).replaceAll("$1=***");
        return STACK_TRACE.matcher(masked).replaceAll("stack_trace=***");
    }
}
