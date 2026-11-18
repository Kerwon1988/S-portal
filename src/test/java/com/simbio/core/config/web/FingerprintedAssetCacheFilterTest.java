package com.simbio.core.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FingerprintedAssetCacheFilterTest {

    private static final String FINGERPRINTED_CSS = "/assets/app.0123456789abcdef.css";
    private final FingerprintedAssetCacheFilter filter = new FingerprintedAssetCacheFilter();

    @Test
    void cachesSuccessfulFingerprintNamedAssetsAsPublicAndImmutable() throws Exception {
        assertThat(cacheControl("GET", FINGERPRINTED_CSS, 200))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(cacheControl("HEAD", "/assets/simbio-logo.0123456789abcdef.png", 200))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(cacheControl("GET", FINGERPRINTED_CSS, 304))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(cacheControl("GET", FINGERPRINTED_CSS, 206))
                .isEqualTo("public, max-age=31536000, immutable");
    }

    @Test
    void doesNotCacheManifestUnfingerprintedOrNonPublicResponses() throws Exception {
        assertThat(cacheControl("GET", "/assets/asset-manifest.json", 200)).isNull();
        assertThat(cacheControl("GET", "/assets/app.css", 200)).isNull();
        assertThat(cacheControl("GET", "/es/home", 200)).isNull();
        assertThat(cacheControl("POST", FINGERPRINTED_CSS, 200)).isNull();
        assertThat(cacheControl("GET", FINGERPRINTED_CSS, 404)).isNull();
    }

    @Test
    void recognizesFingerprintPathsWhenApplicationUsesAContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/portal/assets/app.0123456789abcdef.css");
        request.setContextPath("/portal");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("public, max-age=31536000, immutable");
    }

    private String cacheControl(String method, String path, int status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            MockHttpServletResponse mockResponse = (MockHttpServletResponse) servletResponse;
            mockResponse.setStatus(status);
        });

        return response.getHeader("Cache-Control");
    }
}
