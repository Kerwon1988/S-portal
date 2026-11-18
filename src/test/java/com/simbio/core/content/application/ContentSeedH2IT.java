package com.simbio.core.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import com.simbio.core.content.infrastructure.ContentJpaTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(ContentJpaTestConfiguration.class)
class ContentSeedH2IT {

    @Autowired
    private ContentStore store;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("TC-CMS-012: seed is bilingual, idempotent, and keeps unapproved copy pending")
    void seedIsIdempotentAndLeavesLegalAndCommercialCopyPending() throws Exception {
        ContentSeed seed = new ContentSeed(store, new ObjectMapper());
        ApplicationArguments arguments = new DefaultApplicationArguments(new String[0]);

        seed.run(arguments);
        entityManager.flush();
        int translationsAfterFirstRun = store.findAllTranslations().size();
        seed.run(arguments);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findAllTranslations()).hasSize(translationsAfterFirstRun);
        assertThat(store.findTranslation("home", "es")).get()
                .satisfies(home -> {
                    assertThat(home.getPublishedSnapshotJson()).contains("Menos gestión, más negocio.");
                    assertThat(home.getVersion()).isEqualTo(1);
                });
        assertThat(store.findTranslation("home", "en")).get()
                .satisfies(home -> {
                    assertThat(home.getPublishedSnapshotJson()).contains("Less admin, more business.");
                    assertThat(home.getPublishedSnapshotJson()).contains("Access the portal");
                    assertThat(home.getVersion()).isEqualTo(1);
                });
        assertThat(store.findTranslation("request-conditions", "es")).get()
                .satisfies(copy -> {
                    assertThat(copy.getState().name()).isEqualTo("DRAFT");
                    assertThat(copy.getApprovalState().name()).isEqualTo("PENDING");
                    assertThat(copy.getPublishedSnapshotJson()).isNull();
                });
        assertThat(store.findTranslation("about-us", "es")).get()
                .satisfies(about -> {
                    assertThat(about.getState().name()).isEqualTo("DRAFT");
                    assertThat(about.getApprovalState().name()).isEqualTo("PENDING");
                    assertThat(about.getPublishedSnapshotJson()).isNull();
                });
        assertThat(store.findLegalDocuments()).hasSize(10)
                .allSatisfy(document -> assertThat(document.getApprovalState()).isEqualTo("PENDING"));
        assertThat(store.findLegalDocuments()).filteredOn(document -> document.getDocumentType().equals("MARKETING"))
                .hasSize(2)
                .allSatisfy(document -> assertThat(document.getApprovalState()).isEqualTo("PENDING"));
    }
}
