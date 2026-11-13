package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.simbio.core.shared.spi.ErrorReporter.Source;
import com.simbio.tests.security.SecurityTestEndpoints;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityTestEndpoints.class)
class ErrorTelemetryControllerTest {

    private static final Path STORAGE_DIRECTORY = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "sportal-error-telemetry-" + UUID.randomUUID())
            .toAbsolutePath()
            .normalize();
    private static final Path INVENTORY_PATH = STORAGE_DIRECTORY.resolve("error-inventory.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ErrorInventoryService inventoryService;

    @DynamicPropertySource
    static void configureInventory(DynamicPropertyRegistry registry) {
        registry.add("portal.error-tracking.inventory-path", INVENTORY_PATH::toString);
        registry.add("portal.error-tracking.release", () -> "test-release");
    }

    @Test
    void acceptsCsrfProtectedSameOriginReportAndPersistsOnlySanitizedMetadata() throws Exception {
        CsrfCredentials csrf = csrfCredentials();
        String requestBody = """
                {
                  "eventType": "UNCAUGHT_JS",
                  "errorName": "TypeError",
                  "stack": "at app.0123456789abcdef.js:12:34\\nat /users/ana@example.com?token=secret",
                  "locale": "es",
                  "correlationId": "corr-12345678",
                  "path": "/es/contacto/ana@example.com?email=ana@example.com"
                }
                """;

        mockMvc.perform(post(ErrorTelemetryRequestFilter.ENDPOINT)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .header("Origin", "http://localhost:8090")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isNoContent());

        assertThat(inventoryService.snapshot().errors())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.source()).isEqualTo(Source.FRONTEND);
                    assertThat(record.category()).isEqualTo("UNCAUGHT_JS");
                    assertThat(record.errorCode()).isEqualTo("TypeError");
                    assertThat(record.release()).isEqualTo("test-release");
                    assertThat(record.locale()).isEqualTo("es");
                    assertThat(record.correlationId()).isEqualTo("corr-12345678");
                    assertThat(record.path()).isEqualTo("/es/contacto/{redacted}");
                    assertThat(record.sanitizedStack()).contains("frontend.assets.app.0123456789abcdef");
                });
        String persistedJson = Files.readString(INVENTORY_PATH);
        assertThat(persistedJson)
                .doesNotContain("ana@example.com")
                .doesNotContain("secret")
                .doesNotContain("token=");
    }

    @Test
    void rejectsMissingCsrfForeignOriginInvalidPayloadAndOversizedPayload() throws Exception {
        CsrfCredentials csrf = csrfCredentials();
        String validBody = """
                {"eventType":"UNCAUGHT_JS","errorName":"TypeError","locale":"en","path":"/en"}
                """;

        mockMvc.perform(post(ErrorTelemetryRequestFilter.ENDPOINT)
                        .header("Origin", "http://localhost:8090")
                        .contentType("application/json")
                        .content(validBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ErrorTelemetryRequestFilter.ENDPOINT)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .header("Origin", "https://attacker.example.invalid")
                        .contentType("application/json")
                        .content(validBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ErrorTelemetryRequestFilter.ENDPOINT)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .header("Origin", "http://localhost:8090")
                        .contentType("application/json")
                        .content("{\"eventType\":\"DATABASE\",\"errorName\":\"TypeError\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(ErrorTelemetryRequestFilter.ENDPOINT)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .header("Origin", "http://localhost:8090")
                        .contentType("application/json")
                        .content(" ".repeat(4097)))
                .andExpect(status().isPayloadTooLarge());
    }

    private CsrfCredentials csrfCredentials() throws Exception {
        MvcResult result = mockMvc.perform(get("/public/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SPORTAL_XSRF");
        return new CsrfCredentials(cookie, result.getResponse().getContentAsString());
    }

    @AfterAll
    static void removeInventory() throws IOException {
        Files.deleteIfExists(INVENTORY_PATH);
        Files.deleteIfExists(STORAGE_DIRECTORY);
    }

    private record CsrfCredentials(Cookie cookie, String token) {
    }
}
