package com.simbio.core.config.errortracking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Enumeration;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import com.simbio.core.config.PortalProperties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ErrorTelemetryRequestFilter extends OncePerRequestFilter {

    static final String ENDPOINT = "/api/telemetry/errors";
    static final int MAX_BODY_BYTES = 4096;

    private final URI portalOrigin;

    public ErrorTelemetryRequestFilter(PortalProperties properties) {
        this.portalOrigin = URI.create(properties.baseUrl());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isTelemetryPost(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!isSameOrigin(request.getHeader("Origin"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String contentEncoding = request.getHeader("Content-Encoding");
        if (contentEncoding != null && !"identity".equalsIgnoreCase(contentEncoding)) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        String contentTypeHeader = request.getContentType();
        if (contentTypeHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(contentTypeHeader);
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        byte[] body = readBoundedBody(request);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        filterChain.doFilter(new BoundedBodyRequest(request, body), response);
    }

    private boolean isTelemetryPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + ENDPOINT).equals(request.getRequestURI());
    }

    private boolean isSameOrigin(String originHeader) {
        if (originHeader == null || originHeader.isBlank()) {
            return false;
        }
        try {
            URI origin = URI.create(originHeader);
            return origin.getScheme() != null
                    && origin.getHost() != null
                    && origin.getRawUserInfo() == null
                    && (origin.getRawPath() == null || origin.getRawPath().isEmpty())
                    && origin.getRawQuery() == null
                    && origin.getRawFragment() == null
                    && origin.getScheme().equalsIgnoreCase(portalOrigin.getScheme())
                    && origin.getHost().equalsIgnoreCase(portalOrigin.getHost())
                    && effectivePort(origin) == effectivePort(portalOrigin);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static byte[] readBoundedBody(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(Math.min(MAX_BODY_BYTES, 1024));
        byte[] buffer = new byte[1024];
        ServletInputStream input = request.getInputStream();
        int total = 0;
        int read;
        while ((read = input.read(buffer, 0, Math.min(buffer.length, MAX_BODY_BYTES + 1 - total))) != -1) {
            total += read;
            if (total > MAX_BODY_BYTES) {
                return null;
            }
            body.write(buffer, 0, read);
        }
        return body.toByteArray();
    }

    private static final class BoundedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private BoundedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new IllegalStateException("Non-blocking reads are not supported for telemetry payloads");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Content-Length".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(Integer.toString(body.length)));
            }
            return super.getHeaders(name);
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Length".equalsIgnoreCase(name)) {
                return Integer.toString(body.length);
            }
            return super.getHeader(name);
        }
    }
}
