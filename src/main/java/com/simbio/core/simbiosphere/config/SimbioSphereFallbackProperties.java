package com.simbio.core.simbiosphere.config;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.simbiosphere.fallback")
public record SimbioSphereFallbackProperties(
        boolean enabled,
        URI onboardingUrl,
        Set<String> allowedHosts) {

    public SimbioSphereFallbackProperties {
        allowedHosts = allowedHosts == null ? Set.of() : allowedHosts.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (enabled) {
            if (onboardingUrl == null || onboardingUrl.getHost() == null
                    || !"https".equalsIgnoreCase(onboardingUrl.getScheme())
                    && !isLoopback(onboardingUrl)) {
                throw new IllegalArgumentException("SimbioSphere fallback URL must use HTTPS, except loopback");
            }
            if (onboardingUrl.getUserInfo() != null || onboardingUrl.getRawQuery() != null
                    || onboardingUrl.getRawFragment() != null) {
                throw new IllegalArgumentException("SimbioSphere fallback URL cannot contain credentials, query or fragment");
            }
            if (!allowedHosts.contains(onboardingUrl.getHost().toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("SimbioSphere fallback host is not allowlisted");
            }
        }
    }

    public boolean available() {
        return enabled && onboardingUrl != null;
    }

    private static boolean isLoopback(URI uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1") || host.equals("::1"));
    }
}
