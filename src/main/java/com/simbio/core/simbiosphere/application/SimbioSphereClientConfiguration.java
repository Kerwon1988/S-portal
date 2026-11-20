package com.simbio.core.simbiosphere.application;

import java.net.URI;
import java.time.Duration;

public interface SimbioSphereClientConfiguration {
    boolean isEnabled();
    URI getBaseUrl();
    String getKeyId();
    String getSecret();
    Duration getConnectTimeout();
    Duration getReadTimeout();
    int getCircuitFailureThreshold();
    Duration getCircuitOpenDuration();
    void validateEnabled();
}
