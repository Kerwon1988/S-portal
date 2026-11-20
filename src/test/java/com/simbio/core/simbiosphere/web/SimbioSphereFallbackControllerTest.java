package com.simbio.core.simbiosphere.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.simbio.core.simbiosphere.config.SimbioSphereFallbackProperties;

class SimbioSphereFallbackControllerTest {
    @Test
    void redirectsOnlyWhenFeatureIsEnabledAndHostIsAllowlisted() {
        var properties = new SimbioSphereFallbackProperties(true,
                URI.create("https://orders.simbio.info/onboarding"), Set.of("orders.simbio.info"));

        var response = new SimbioSphereFallbackController(properties).continueExternally();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://orders.simbio.info/onboarding"));
    }

    @Test
    void disabledFeatureDoesNotRedirect() {
        var properties = new SimbioSphereFallbackProperties(false, null, Set.of("orders.simbio.info"));

        assertThat(new SimbioSphereFallbackController(properties).continueExternally().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsOpenRedirectQueriesCredentialsAndUnknownHosts() {
        assertThatThrownBy(() -> new SimbioSphereFallbackProperties(true,
                URI.create("https://evil.example/onboarding"), Set.of("orders.simbio.info")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimbioSphereFallbackProperties(true,
                URI.create("https://orders.simbio.info/onboarding?email=synthetic@example.test"),
                Set.of("orders.simbio.info")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
