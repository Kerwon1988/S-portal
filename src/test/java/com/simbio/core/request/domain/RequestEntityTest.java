package com.simbio.core.request.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;

class RequestEntityTest {
    @Test
    void createsReceivedRequestWithSyntheticIds() {
        RequestEntity request = request();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.RECIBIDA);
        assertThat(request.getReferenceCode()).matches("SOL-\\d{4}-\\d{6}");
    }

    @Test
    void rejectsMissingOwnerAndCategory() {
        assertThatThrownBy(() -> new RequestEntity(UUID.randomUUID(), "SOL-2026-000001",
                RequestType.CUSTOM_DEVELOPMENT, "", "Title", "Description", null, null, null,
                null, null, null, UUID.randomUUID(), Instant.EPOCH))
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }

    private static RequestEntity request() {
        return new RequestEntity(UUID.randomUUID(), "SOL-2026-000001",
                RequestType.CUSTOM_DEVELOPMENT, "WEB_APPLICATION", "Synthetic request", "Synthetic description",
                "Goal", null, null, null, null, UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
    }
}
