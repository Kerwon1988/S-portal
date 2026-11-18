package com.simbio.core.config.web;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FingerprintedAssetCacheFilter extends OncePerRequestFilter {

    private static final Set<String> CACHEABLE_METHODS = Set.of("GET", "HEAD");
    private static final Pattern FINGERPRINTED_ASSET =
            Pattern.compile("^/assets/[a-zA-Z0-9_-]+\\.[0-9a-f]{16}\\.(?:css|js|png)$");
    private static final String IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !CACHEABLE_METHODS.contains(request.getMethod())
                || !FINGERPRINTED_ASSET.matcher(pathWithinApplication(request)).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, IMMUTABLE_CACHE_CONTROL);
        filterChain.doFilter(request, response);
        int status = response.getStatus();
        if (status != HttpServletResponse.SC_OK
                && status != HttpServletResponse.SC_PARTIAL_CONTENT
                && status != HttpServletResponse.SC_NOT_MODIFIED
                && !response.isCommitted()) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, null);
        }
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return request.getRequestURI().substring(contextPath.length());
    }
}
