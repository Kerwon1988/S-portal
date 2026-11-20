package com.simbio.core.simbiosphere.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LinkedBusinessEntityTest {
    @Test
    void oneAccountLinkCanOwnMultipleBusinessReferences() {
        AccountLinkEntity link = new AccountLinkEntity(UUID.randomUUID(), UUID.randomUUID(),
                "SIMBIOSPHERE", "external-account", Instant.parse("2026-11-20T18:00:00Z"));
        LinkedBusinessEntity first = new LinkedBusinessEntity(UUID.randomUUID(), link,
                "business-1", "Synthetic one", "RESTAURANT", "one", Instant.now());
        LinkedBusinessEntity second = new LinkedBusinessEntity(UUID.randomUUID(), link,
                "business-2", "Synthetic two", "REPAIR_SHOP", "two", Instant.now());

        assertThat(first.getAccountLink()).isSameAs(link);
        assertThat(second.getAccountLink()).isSameAs(link);
        assertThat(first.getExternalBusinessId()).isNotEqualTo(second.getExternalBusinessId());
    }
}
