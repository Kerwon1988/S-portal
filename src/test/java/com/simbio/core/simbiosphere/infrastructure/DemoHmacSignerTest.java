package com.simbio.core.simbiosphere.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class DemoHmacSignerTest {
    @Test
    void signsApprovedCanonicalVector() {
        String signature = new DemoHmacSigner().sign("synthetic-secret", "1700000000", "GET",
                URI.create("/api/platform/v1/tenant-slugs/demo-2/availability"), new byte[0], null);

        assertThat(signature).isEqualTo(
                "v1=49083fa18d06f3b5edcdfe6b156ef4ac522f6afdfa8fc84992683e690020a732");
    }

    @Test
    void canonicalizesQueryAndIncludesIdempotencyKey() {
        String canonical = DemoHmacSigner.canonical("1700000000", "post",
                URI.create("/api/platform/v1/owner-onboardings?z=last&a=first"), "{}".getBytes(),
                "external-uuid-1");

        assertThat(canonical).startsWith(
                "1700000000\nPOST\n/api/platform/v1/owner-onboardings\na=first&z=last\n")
                .endsWith("\nexternal-uuid-1");
    }
}
