package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.simbio.core.config.PortalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

class ErrorTelemetryRequestFilterTest {

    private final ErrorTelemetryRequestFilter filter =
            new ErrorTelemetryRequestFilter(new PortalProperties("https://portal.example.invalid", "Simbio"));

    @Test
    void permitsOnlyBoundedSameOriginJsonPosts() throws Exception {
        MockHttpServletRequest request = request();
        request.setContent("{\"eventType\":\"UNCAUGHT_JS\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(chain.getRequest()).isInstanceOf(HttpServletRequest.class);
        if (chain.getRequest() instanceof HttpServletRequest wrappedRequest) {
            assertThat(wrappedRequest.getContentLength()).isLessThanOrEqualTo(4096);
        }
    }

    @Test
    void rejectsForeignOriginsMissingJsonAndOversizedBodies() throws Exception {
        MockHttpServletRequest foreignRequest = request("https://attacker.example.invalid");
        MockHttpServletResponse foreignResponse = new MockHttpServletResponse();
        MockFilterChain foreignChain = new MockFilterChain();

        filter.doFilter(foreignRequest, foreignResponse, foreignChain);

        assertThat(foreignResponse.getStatus()).isEqualTo(403);
        assertThat(foreignChain.getRequest()).isNull();

        MockHttpServletRequest noJsonRequest = request();
        noJsonRequest.setContentType("text/plain");
        MockHttpServletResponse noJsonResponse = new MockHttpServletResponse();
        filter.doFilter(noJsonRequest, noJsonResponse, new MockFilterChain());
        assertThat(noJsonResponse.getStatus()).isEqualTo(415);

        MockHttpServletRequest oversizedRequest = request();
        oversizedRequest.setContent(new byte[4097]);
        MockHttpServletResponse oversizedResponse = new MockHttpServletResponse();
        MockFilterChain oversizedChain = new MockFilterChain();

        filter.doFilter(oversizedRequest, oversizedResponse, oversizedChain);

        assertThat(oversizedResponse.getStatus()).isEqualTo(413);
        assertThat(oversizedChain.getRequest()).isNull();
    }

    private static MockHttpServletRequest request() {
        return request("https://portal.example.invalid");
    }

    private static MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", ErrorTelemetryRequestFilter.ENDPOINT);
        request.setRequestURI(ErrorTelemetryRequestFilter.ENDPOINT);
        request.setContentType("application/json");
        request.addHeader("Origin", origin);
        return request;
    }
}
