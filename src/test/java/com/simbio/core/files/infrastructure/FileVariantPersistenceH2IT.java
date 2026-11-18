package com.simbio.core.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.application.FileMetadataStore;
import com.simbio.core.files.application.FileVariantStore;
import com.simbio.core.files.domain.FileMetadataEntity;
import com.simbio.core.files.domain.FileVariantEntity;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true")
@ActiveProfiles("test")
@Import({JpaFileMetadataStore.class, JpaFileVariantStore.class})
class FileVariantPersistenceH2IT {

    @Autowired
    private FileMetadataStore metadataStore;

    @Autowired
    private FileVariantStore variantStore;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsVariantMetadataForAnOwnedFile() {
        UUID publicId = UUID.randomUUID();
        FileMetadataEntity parent = metadataStore.save(new FileMetadataEntity(
                publicId, "0123456789abcdef0123456789abcdef", "upload.png", "image/png",
                300, ScanState.CLEAN, Instant.parse("2026-11-17T00:00:00Z")));
        entityManager.flush();

        variantStore.save(new FileVariantEntity(
                parent, "w400", "abcdef0123456789abcdef0123456789", 400, 200, "image/webp", 128));
        entityManager.flush();
        entityManager.clear();

        assertThat(variantStore.findByFileId(parent.getId()))
                .singleElement()
                .satisfies(variant -> {
                    assertThat(variant.getVariantCode()).isEqualTo("w400");
                    assertThat(variant.getMime()).isEqualTo("image/webp");
                    assertThat(variant.getSizeBytes()).isEqualTo(128);
                });
    }
}
