package com.simbio.core.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;

import com.simbio.core.content.application.ContentStore;
import com.simbio.core.content.domain.ApprovalState;
import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.LegalDocumentVersionEntity;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.TranslationState;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaContentStore.class)
class ContentPersistenceH2IT {

    @Autowired
    private ContentStore store;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("TC-CMS-001: H2 persists owned pages with Spanish and English translations")
    void persistsPageAndSpanishEnglishTranslationsInH2() {
        PageEntity page = new PageEntity(UUID.randomUUID(), "company", ContentPageType.PAGE,
                Instant.parse("2026-11-17T00:00:00Z"));
        var spanish = page.translation("es");
        spanish.edit("sobre-nosotros", "Sobre nosotros", "Sobre nosotros", "Descripción", ApprovalState.PENDING,
                UUID.randomUUID(), Instant.parse("2026-11-17T00:01:00Z"));
        var english = page.translation("en");
        english.edit("about-us", "About us", "About us", "Description", ApprovalState.PENDING,
                UUID.randomUUID(), Instant.parse("2026-11-17T00:01:00Z"));
        store.savePage(page);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findTranslation("company", "es")).get()
                .satisfies(translation -> {
                    assertThat(translation.getSlug()).isEqualTo("sobre-nosotros");
                    assertThat(translation.getState()).isEqualTo(TranslationState.DRAFT);
                });
        assertThat(store.findTranslation("company", "en")).get()
                .extracting("slug")
                .isEqualTo("about-us");
    }

    @Test
    @DisplayName("TC-CMS-001: locale slug uniqueness rejects duplicates without overwriting")
    void uniqueLocaleSlugRejectsDuplicateWithoutOverwritingExistingTranslation() {
        PageEntity first = new PageEntity(UUID.randomUUID(), "first-page", ContentPageType.PAGE, Instant.now());
        first.translation("es").edit("inicio", "Inicio", "Inicio", "", ApprovalState.PENDING,
                UUID.randomUUID(), Instant.now());
        store.savePage(first);
        entityManager.flush();

        PageEntity second = new PageEntity(UUID.randomUUID(), "second-page", ContentPageType.PAGE, Instant.now());
        second.translation("es").edit("inicio", "Other", "Other", "", ApprovalState.PENDING,
                UUID.randomUUID(), Instant.now());
        assertThatThrownBy(() -> {
            store.savePage(second);
            entityManager.flush();
        }).isInstanceOfAny(ConstraintViolationException.class,
                org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("TC-CMS-010: a future legal version does not replace the current one early")
    void futureApprovedLegalVersionLeavesCurrentVersionInPlaceUntilEffectiveDate() {
        Instant effectiveFrom = Instant.parse("2030-01-01T00:00:00Z");
        store.saveLegalDocument(new LegalDocumentVersionEntity(UUID.randomUUID(), "PRIVACY", "es",
                1, "Previous privacy notice", "Previous approved content", "APPROVED", null,
                UUID.randomUUID(), Instant.parse("2025-01-01T00:00:00Z")));
        store.saveLegalDocument(new LegalDocumentVersionEntity(UUID.randomUUID(), "PRIVACY", "es",
                2, "Privacy notice", "Approved content", "APPROVED", effectiveFrom,
                UUID.randomUUID(), Instant.parse("2026-11-17T00:00:00Z")));
        entityManager.flush();

        assertThat(store.findCurrentLegalDocument("PRIVACY", "es", effectiveFrom.minusSeconds(1))).get()
                .satisfies(document -> assertThat(document.getVersion()).isEqualTo(1));
        assertThat(store.findCurrentLegalDocument("PRIVACY", "es", effectiveFrom)).get()
                .satisfies(document -> assertThat(document.getVersion()).isEqualTo(2));
    }
}
