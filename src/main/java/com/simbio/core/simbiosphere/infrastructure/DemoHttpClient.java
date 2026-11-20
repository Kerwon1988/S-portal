package com.simbio.core.simbiosphere.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.simbio.core.simbiosphere.application.DemoClientException;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class DemoHttpClient implements DemoProvisioningClient {
    private final SimbioSphereClientProperties properties;
    private final ObjectMapper objectMapper;
    private final DemoHmacSigner signer;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DemoHttpClient(SimbioSphereClientProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout()).build(),
                new DemoHmacSigner(), new CircuitBreaker(properties.getCircuitFailureThreshold(),
                        properties.getCircuitOpenDuration(), Clock.systemUTC()), Clock.systemUTC());
    }

    DemoHttpClient(SimbioSphereClientProperties properties, ObjectMapper objectMapper,
            HttpClient httpClient, DemoHmacSigner signer, CircuitBreaker circuitBreaker, Clock clock) {
        properties.validateEnabled();
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = Objects.requireNonNull(httpClient);
        this.signer = signer;
        this.circuitBreaker = circuitBreaker;
        this.clock = clock;
    }

    @Override
    public OwnerOnboardingResult provision(OwnerOnboardingCommand command, String idempotencyKey) {
        byte[] body = json(command);
        return exchange("POST", URI.create("/api/platform/v1/owner-onboardings"), body,
                idempotencyKey, OwnerOnboardingResult.class);
    }

    @Override
    public SlugAvailability checkSlug(String slug) {
        if (slug == null || !slug.matches("[a-z0-9-]{3,50}")) {
            throw new IllegalArgumentException("Invalid SimbioSphere slug");
        }
        return exchange("GET", URI.create("/api/platform/v1/tenant-slugs/" + slug + "/availability"),
                new byte[0], null, SlugAvailability.class);
    }

    @Override
    public ActivationLinkResult createActivationLink(long customerAccountId, String idempotencyKey) {
        if (customerAccountId < 1) throw new IllegalArgumentException("Customer account id must be positive");
        return exchange("POST", URI.create("/api/platform/v1/owner-onboardings/"
                + customerAccountId + "/activation-links"), new byte[0], idempotencyKey,
                ActivationLinkResult.class);
    }

    private <T> T exchange(String method, URI relativePath, byte[] body, String idempotencyKey,
            Class<T> responseType) {
        requireEnabled();
        if (!circuitBreaker.allowRequest()) {
            throw new DemoClientException(503, "CIRCUIT_OPEN", null, true, null);
        }
        String timestamp = Long.toString(Instant.now(clock).getEpochSecond());
        String signature = signer.sign(properties.getSecret(), timestamp, method, relativePath,
                body, idempotencyKey);
        var requestBuilder = HttpRequest.newBuilder(properties.getBaseUrl().resolve(relativePath))
                .timeout(properties.getReadTimeout())
                .header("X-Platform-Key-Id", properties.getKeyId())
                .header("X-Platform-Timestamp", timestamp)
                .header("X-Platform-Signature", signature)
                .header("Accept", "application/json");
        if (idempotencyKey != null) requestBuilder.header("Idempotency-Key", idempotencyKey);
        HttpRequest request = "POST".equals(method)
                ? requestBuilder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
                : requestBuilder.GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                T value = objectMapper.readValue(response.body(), responseType);
                circuitBreaker.recordSuccess();
                return value;
            }
            Problem problem = parseProblem(status, response.body());
            if (status >= 500 || status == 429) circuitBreaker.recordFailure();
            throw new DemoClientException(status, problem.code(), problem.correlationId(),
                    status == 429 || status >= 500, null);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            circuitBreaker.recordFailure();
            throw new DemoClientException(503, "TRANSPORT_UNAVAILABLE", null, true, exception);
        }
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("SimbioSphere payload could not be encoded", exception);
        }
    }

    private Problem parseProblem(int status, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            return new Problem(root.path("code").asText("UNKNOWN"), root.path("correlationId").asText(null));
        } catch (RuntimeException exception) {
            return new Problem(status >= 500 ? "DEPENDENCY_UNAVAILABLE" : "HTTP_ERROR", null);
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new DemoClientException(503, "CLIENT_DISABLED", null, false, null);
        }
    }

    private record Problem(String code, String correlationId) { }
}
