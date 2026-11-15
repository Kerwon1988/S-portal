package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void limitsByRemoteAddressAndIgnoresUntrustedForwardedHeaders() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new StaticMessageSource(), new RateLimitProperties());
        AtomicInteger passed = new AtomicInteger();
        MockHttpServletResponse lastResponse = new MockHttpServletResponse();
        for (int attempt = 0; attempt < 31; attempt++) {
            MockHttpServletRequest request = postRequest();
            request.addHeader("X-Forwarded-For", "203.0.113." + attempt);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> passed.incrementAndGet());
            lastResponse = response;
        }

        assertThat(passed).isEqualTo(30);
        assertThat(lastResponse.getStatus()).isEqualTo(429);
        assertThat(lastResponse.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void appliesSeparateEmailQuotaWithoutReturningSubmittedAddress() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new StaticMessageSource(), new RateLimitProperties());
        AtomicInteger passed = new AtomicInteger();
        MockHttpServletResponse lastResponse = new MockHttpServletResponse();
        for (int attempt = 0; attempt < 6; attempt++) {
            MockHttpServletRequest request = postRequest();
            request.setRemoteAddr("192.0.2." + attempt);
            request.addParameter("username", "synthetic@example.test");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> passed.incrementAndGet());
            lastResponse = response;
        }

        assertThat(passed).isEqualTo(5);
        assertThat(lastResponse.getStatus()).isEqualTo(429);
        assertThat(lastResponse.getContentAsString()).doesNotContain("synthetic@example.test");
    }

    private static MockHttpServletRequest postRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/acceso");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
