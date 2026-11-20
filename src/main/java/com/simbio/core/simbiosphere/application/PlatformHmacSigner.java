package com.simbio.core.simbiosphere.application;

import java.net.URI;

public interface PlatformHmacSigner {
    String sign(String secret, String timestamp, String method, URI target,
            byte[] body, String idempotencyKey);
}
