package com.simbio.core.simbiosphere.infrastructure;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.simbiosphere.client")
public final class SimbioSphereClientProperties
        implements com.simbio.core.simbiosphere.application.SimbioSphereClientConfiguration {
    private boolean enabled;
    private URI baseUrl;
    private String keyId;
    private String secret;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    private int circuitFailureThreshold = 5;
    private Duration circuitOpenDuration = Duration.ofSeconds(30);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration value) { connectTimeout = positive(value, "Connect timeout"); }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration value) { readTimeout = positive(value, "Read timeout"); }
    public int getCircuitFailureThreshold() { return circuitFailureThreshold; }
    public void setCircuitFailureThreshold(int value) {
        if (value < 1) throw new IllegalArgumentException("Circuit failure threshold must be positive");
        circuitFailureThreshold = value;
    }
    public Duration getCircuitOpenDuration() { return circuitOpenDuration; }
    public void setCircuitOpenDuration(Duration value) {
        circuitOpenDuration = positive(value, "Circuit open duration");
    }

    public void validateEnabled() {
        if (!enabled) return;
        if (baseUrl == null || baseUrl.getHost() == null || !baseUrl.getRawPath().isEmpty()
                && !"/".equals(baseUrl.getRawPath())) {
            throw new IllegalArgumentException("SimbioSphere base URL must be an origin only");
        }
        if (!"https".equalsIgnoreCase(baseUrl.getScheme())
                && !("http".equalsIgnoreCase(baseUrl.getScheme()) && isLoopback(baseUrl.getHost()))) {
            throw new IllegalArgumentException("SimbioSphere URL must use HTTPS outside loopback");
        }
        if (keyId == null || keyId.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("SimbioSphere key id and secret are required when enabled");
        }
    }

    private static Duration positive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    @Override public String toString() { return "SimbioSphereClientProperties[secret=<redacted>]"; }
}
