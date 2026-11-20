package com.simbio.core.simbiosphere.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.simbio.core.simbiosphere.application.DemoClientException;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;

import tools.jackson.databind.ObjectMapper;

class DemoHttpClientContractTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsApprovedSignedRequestAndMapsTypedResponse() throws Exception {
        AtomicReference<HttpExchange> exchange = new AtomicReference<>();
        start(http -> {
            exchange.set(http);
            respond(http, 200, "{\"slug\":\"synthetic-shop\",\"available\":true,\"suggestions\":[]}");
        });
        DemoHttpClient client = client(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));

        var result = client.checkSlug("synthetic-shop");

        assertThat(result.available()).isTrue();
        var request = exchange.get().getRequestHeaders();
        assertThat(request.getFirst("X-Platform-Key-Id")).isEqualTo("synthetic-key");
        assertThat(request.getFirst("X-Platform-Timestamp")).matches("[0-9]{10}");
        assertThat(request.getFirst("X-Platform-Signature")).startsWith("v1=");
    }

    @Test
    void mapsSanitizedProblemWithoutLeakingBodyAndMarksFailureRetryable() throws Exception {
        start(http -> respond(http, 503, "{\"type\":\"/problems/unavailable\","
                + "\"title\":\"Unavailable\",\"status\":503,\"code\":\"UPSTREAM_TIMEOUT\","
                + "\"detail\":\"synthetic body must not escape\"}"));
        DemoHttpClient client = client(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));

        assertThatThrownBy(() -> client.checkSlug("synthetic-shop"))
                .isInstanceOf(DemoClientException.class)
                .satisfies(error -> {
                    DemoClientException value = (DemoClientException) error;
                    assertThat(value.status()).isEqualTo(503);
                    assertThat(value.code()).isEqualTo("UPSTREAM_TIMEOUT");
                    assertThat(value.retryable()).isTrue();
                    assertThat(value.getMessage()).doesNotContain("synthetic body");
                });
    }

    @Test
    void includesIdempotencyKeyAndNeverSendsPasswordField() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        start(http -> {
            try {
                body.set(new String(http.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            key.set(http.getRequestHeaders().getFirst("Idempotency-Key"));
            respond(http, 201, "{\"externalAccountId\":\"portal-account-1\",\"customerAccountId\":1,"
                    + "\"tenantId\":2,\"tenantSlug\":\"synthetic-shop\",\"modulesApplied\":[],\"status\":\"PENDING_ACTIVATION\"}");
        });
        DemoHttpClient client = client(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        var command = new DemoProvisioningClient.OwnerOnboardingCommand(
                "synthetic@example.test", "Synthetic", "Owner", "en",
                new DemoProvisioningClient.Verification("VERIFIED", "2026-11-20T18:00:00Z", "EMAIL_OTP"),
                "Synthetic account", "Synthetic shop", "synthetic-shop", "SERVICES", null,
                "Main", "main", new DemoProvisioningClient.Terms("1", "2026-11-20T18:00:00Z"),
                new DemoProvisioningClient.ExternalReference("S_PORTAL", "portal-account-1"));

        client.provision(command, "portal-account-1");

        assertThat(key.get()).isEqualTo("portal-account-1");
        assertThat(body.get()).doesNotContain("password").doesNotContain("token");
    }

    private DemoHttpClient client(URI baseUrl) {
        SimbioSphereClientProperties properties = new SimbioSphereClientProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(baseUrl);
        properties.setKeyId("synthetic-key");
        properties.setSecret("synthetic-secret");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setCircuitFailureThreshold(2);
        return new DemoHttpClient(properties, new ObjectMapper(), HttpClient.newHttpClient(),
                new DemoHmacSigner(), new CircuitBreaker(2, Duration.ofSeconds(30), Clock.systemUTC()),
                Clock.systemUTC());
    }

    private void start(java.util.function.Consumer<HttpExchange> handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", http -> {
            try { handler.accept(http); } catch (Exception exception) { throw new IllegalStateException(exception); }
        });
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        try {
            byte[] response = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
