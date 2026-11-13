package com.simbio.core.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0"})
@ActiveProfiles("test")
class ActuatorExposureIT {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalManagementPort
    private int managementPort;

    @LocalServerPort
    private int applicationPort;

    @Test
    void exposesReadinessOnlyOnManagementPortWithoutDetails() throws Exception {
        HttpResponse<String> readiness = get("http://localhost:" + managementPort + "/actuator/health/readiness");
        HttpResponse<String> environment = get("http://localhost:" + managementPort + "/actuator/env");
        HttpResponse<String> prometheus = get("http://localhost:" + managementPort + "/actuator/prometheus");
        HttpResponse<String> applicationHealth = get("http://localhost:" + applicationPort + "/actuator/health");

        assertThat(readiness.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(readiness.body()).contains("\"status\":\"UP\"").doesNotContain("components");
        assertThat(environment.statusCode())
                .isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.FORBIDDEN.value());
        assertThat(prometheus.statusCode())
                .isIn(HttpStatus.FORBIDDEN.value(), HttpStatus.NOT_FOUND.value());
        assertThat(applicationHealth.statusCode())
                .isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.FORBIDDEN.value());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
