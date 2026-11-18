package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.identity.application.AdminBootstrapService;
import com.simbio.tests.security.SecurityTestEndpoints;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "spring.datasource.url=jdbc:h2:mem:session-cookie;DB_CLOSE_DELAY=-1;NON_KEYWORDS=IDENTITY,ROLE"
        })
@ActiveProfiles("test")
@Import(SecurityTestEndpoints.class)
class SessionCookieIT {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private AdminBootstrapService bootstrapService;

    @LocalServerPort
    private int applicationPort;

    @Test
    void usesPortalSessionCookieWithSecureAttributes() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + applicationPort + "/public/session"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Set-Cookie").orElse(null))
                .contains("SPORTAL_SESSION=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .doesNotContain("JSESSIONID");
    }

    @Test
    void rotatesSessionIdAfterAuthentication() throws Exception {
        bootstrapService.bootstrap("session-admin@example.test", "synthetic-session-password", "en");
        HttpResponse<String> anonymousSessionResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + applicationPort + "/public/session"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        String anonymousSessionId = cookieValue(anonymousSessionResponse, "SPORTAL_SESSION");

        HttpResponse<String> csrfResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + applicationPort + "/public/csrf"))
                        .header("Cookie", "SPORTAL_SESSION=" + anonymousSessionId)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        String csrfToken = csrfResponse.body();
        String csrfCookie = cookieValue(csrfResponse, "SPORTAL_XSRF");
        String form = "username=" + URLEncoder.encode("session-admin@example.test", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("synthetic-session-password", StandardCharsets.UTF_8);
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + applicationPort + "/acceso"))
                        .header("Cookie", "SPORTAL_SESSION=" + anonymousSessionId + "; SPORTAL_XSRF=" + csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);
        assertThat(cookieValue(loginResponse, "SPORTAL_SESSION")).isNotEqualTo(anonymousSessionId);
    }

    private static String cookieValue(HttpResponse<?> response, String name) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .filter(cookie -> cookie.startsWith(name + "="))
                .map(cookie -> cookie.substring(name.length() + 1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected cookie " + name));
    }
}
