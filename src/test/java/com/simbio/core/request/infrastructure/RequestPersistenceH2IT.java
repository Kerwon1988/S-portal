package com.simbio.core.request.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.RequestStore;
import com.simbio.core.request.domain.RequestEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaRequestStore.class)
class RequestPersistenceH2IT {
    @Autowired RequestStore store;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void filtersOnlyByOwningAccountAndStableOrder() {
        UUID owner = UUID.randomUUID();
        RequestEntity first = request(owner, "SOL-2026-000001");
        RequestEntity second = request(owner, "SOL-2026-000002");
        RequestEntity foreign = request(UUID.randomUUID(), "SOL-2026-000003");
        store.save(first); store.save(second); store.save(foreign);
        entityManager.flush();
        entityManager.clear();

        var page = store.searchOwned(owner, RequestType.CUSTOM_DEVELOPMENT, null, "", null, null,
                PageRequest.of(0, 10, Sort.by("createdAt").descending().and(Sort.by("id").descending())));

        assertThat(page.getContent()).extracting(RequestEntity::getPublicId)
                .containsExactly(second.getPublicId(), first.getPublicId());
        assertThat(page.getContent()).noneMatch(value -> value.getPublicId().equals(foreign.getPublicId()));
    }

    private static RequestEntity request(UUID owner, String reference) {
        return new RequestEntity(UUID.randomUUID(), reference, RequestType.CUSTOM_DEVELOPMENT,
                "WEB_APPLICATION", "Synthetic title", "Synthetic description", null, null, null,
                null, null, owner, UUID.randomUUID(), Instant.now());
    }
}
