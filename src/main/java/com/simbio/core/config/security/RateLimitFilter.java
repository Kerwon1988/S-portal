package com.simbio.core.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Metrics;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final MessageSource messageSource;
    private final RateLimitProperties properties;

    public RateLimitFilter(MessageSource messageSource, RateLimitProperties properties) {
        this.messageSource = messageSource;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !properties.protectedPaths().contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant now = Instant.now();
        evictExpired(now);
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()
                || !consume("ip:" + digest(remoteAddress), properties.ipCapacity(), now)) {
            reject(request, response);
            return;
        }

        String email = request.getParameter("username");
        if (email == null) {
            email = request.getParameter("email");
        }
        if (email != null && !email.isBlank()
                && !consume("email:" + digest(email.trim().toLowerCase(Locale.ROOT)),
                        properties.emailCapacity(), now)) {
            reject(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean consume(String key, int capacity, Instant now) {
        BucketState state = buckets.get(key);
        if (state == null && buckets.size() >= properties.maxBuckets()) {
            evictExpired(now);
            if (buckets.size() >= properties.maxBuckets()) {
                return false;
            }
        }
        state = buckets.computeIfAbsent(key, ignored -> new BucketState(
                Bucket.builder().addLimit(limit -> limit.capacity(capacity)
                        .refillGreedy(capacity, properties.window())).build(), now));
        state.lastTouched = now;
        return state.bucket.tryConsume(1);
    }

    private void evictExpired(Instant now) {
        Instant expiry = now.minus(properties.window().multipliedBy(2));
        buckets.entrySet().removeIf(entry -> entry.getValue().lastTouched.isBefore(expiry));
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Metrics.counter("portal.security.rate-limit.rejected").increment();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(Math.max(1, properties.window().toSeconds())));
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(messageSource.getMessage("security.rate-limit", null,
                "Too many requests. Try again later.", request.getLocale()));
    }

    private static String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }

    private static final class BucketState {
        private final Bucket bucket;
        private volatile Instant lastTouched;

        private BucketState(Bucket bucket, Instant lastTouched) {
            this.bucket = bucket;
            this.lastTouched = lastTouched;
        }
    }
}
