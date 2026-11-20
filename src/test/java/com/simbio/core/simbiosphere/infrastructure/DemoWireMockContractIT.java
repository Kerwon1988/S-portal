package com.simbio.core.simbiosphere.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.simbio.core.simbiosphere.application.DemoClientException;

import tools.jackson.databind.ObjectMapper;

class DemoWireMockContractIT {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Test
    void validatesSignedSlugRequestAgainstContractFixture() {
        wireMock.stubFor(get(urlPathEqualTo("/api/platform/v1/tenant-slugs/synthetic/availability"))
                .withHeader("X-Platform-Key-Id", matching("synthetic-key"))
                .withHeader("X-Platform-Timestamp", matching("[0-9]{10}"))
                .withHeader("X-Platform-Signature", matching("v1=[a-f0-9]{64}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Cache-Control", "no-store")
                        .withBody("{\"slug\":\"synthetic\",\"available\":true,\"suggestions\":[]}")));

        assertThat(client().checkSlug("synthetic").available()).isTrue();
    }

    @Test
    void classifiesContractProblemDetailsAndDoesNotLeakBody() {
        wireMock.stubFor(get(urlPathEqualTo("/api/platform/v1/tenant-slugs/unavailable/availability"))
                .willReturn(aResponse().withStatus(503)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"type\":\"/problems/unavailable\",\"title\":\"Unavailable\","
                                + "\"status\":503,\"code\":\"DEPENDENCY_UNAVAILABLE\","
                                + "\"detail\":\"sensitive upstream body\"}")));

        assertThatThrownBy(() -> client().checkSlug("unavailable"))
                .isInstanceOfSatisfying(DemoClientException.class, error -> {
                    assertThat(error.status()).isEqualTo(503);
                    assertThat(error.code()).isEqualTo("DEPENDENCY_UNAVAILABLE");
                    assertThat(error.getMessage()).doesNotContain("sensitive upstream body");
                });
    }

    private static DemoHttpClient client() {
        SimbioSphereClientProperties properties = new SimbioSphereClientProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(URI.create(wireMock.baseUrl()));
        properties.setKeyId("synthetic-key");
        properties.setSecret("synthetic-secret");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(2));
        return new DemoHttpClient(properties, new ObjectMapper(), java.net.http.HttpClient.newHttpClient(),
                new DemoHmacSigner(), new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC()),
                Clock.systemUTC());
    }
}
