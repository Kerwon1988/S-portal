package com.simbio.core.simbiosphere.infrastructure;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class DemoHmacSigner implements com.simbio.core.simbiosphere.application.PlatformHmacSigner {
    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String secret, String timestamp, String method, URI target,
            byte[] body, String idempotencyKey) {
        String canonical = canonical(timestamp, method, target, body, idempotencyKey);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return "v1=" + HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    static String canonical(String timestamp, String method, URI target, byte[] body,
            String idempotencyKey) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body == null ? new byte[0] : body));
            return String.join("\n",
                    timestamp,
                    method.toUpperCase(java.util.Locale.ROOT),
                    target.getRawPath(),
                    canonicalQuery(target.getRawQuery()),
                    digest,
                    idempotencyKey == null ? "" : idempotencyKey);
        } catch (Exception exception) {
            throw new IllegalStateException("Canonical request hashing failed", exception);
        }
    }

    static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return "";
        return java.util.Arrays.stream(rawQuery.split("&"))
                .map(DemoHmacSigner::queryPair)
                .sorted()
                .collect(Collectors.joining("&"));
    }

    private static String queryPair(String pair) {
        int equals = pair.indexOf('=');
        String name = equals < 0 ? pair : pair.substring(0, equals);
        String value = equals < 0 ? "" : pair.substring(equals + 1);
        return name + "=" + value;
    }
}
