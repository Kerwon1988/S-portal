package com.simbio.core.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.application.FileMetadataStore;
import com.simbio.core.files.domain.FileMetadataEntity;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true")
@Import(JpaFileMetadataStore.class)
class FileMetadataPersistenceH2IT {

    @Autowired
    private FileMetadataStore metadataStore;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReloadsFileMetadataWithGeneratedH2Schema() {
        UUID publicId = UUID.randomUUID();
        metadataStore.save(new FileMetadataEntity(
                publicId, "0123456789abcdef0123456789abcdef", "image/png", 3, ScanState.QUARANTINED,
                Instant.parse("2026-11-13T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertThat(metadataStore.findByPublicId(publicId))
                .get()
                .satisfies(metadata -> {
                    assertThat(metadata.getStorageKey()).isEqualTo("0123456789abcdef0123456789abcdef");
                    assertThat(metadata.getScanState()).isEqualTo(ScanState.QUARANTINED);
                });
    }
}
