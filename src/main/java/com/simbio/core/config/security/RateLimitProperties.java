package com.simbio.core.config.security;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.security.rate-limit")
public class RateLimitProperties {

    private int maxBuckets = 10_000;
    private int ipCapacity = 30;
    private int emailCapacity = 5;
    private Duration window = Duration.ofMinutes(1);

    public int maxBuckets() {
        return maxBuckets;
    }

    public void setMaxBuckets(int maxBuckets) {
        if (maxBuckets < 1) {
            throw new IllegalArgumentException("Rate-limit bucket capacity must be positive");
        }
        this.maxBuckets = maxBuckets;
    }

    public int ipCapacity() {
        return ipCapacity;
    }

    public void setIpCapacity(int ipCapacity) {
        if (ipCapacity < 1) {
            throw new IllegalArgumentException("IP rate limit must be positive");
        }
        this.ipCapacity = ipCapacity;
    }

    public int emailCapacity() {
        return emailCapacity;
    }

    public void setEmailCapacity(int emailCapacity) {
        if (emailCapacity < 1) {
            throw new IllegalArgumentException("Email rate limit must be positive");
        }
        this.emailCapacity = emailCapacity;
    }

    public Duration window() {
        return window;
    }

    public void setWindow(Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }
        this.window = window;
    }

    public Set<String> protectedPaths() {
        return Set.of("/acceso", "/registro", "/recuperar", "/restablecer", "/contacto",
                "/es/contacto", "/en/contacto");
    }
}
