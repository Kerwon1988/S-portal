package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.content.domain.TranslationState;

interface PageTranslationJpaRepository extends JpaRepository<PageTranslationEntity, Long> {

    Optional<PageTranslationEntity> findByPage_PageKeyAndLocale(String pageKey, String locale);

    Optional<PageTranslationEntity> findByPublicId(UUID publicId);

    Optional<PageTranslationEntity> findByLocaleAndSlug(String locale, String slug);

    Optional<PageTranslationEntity> findByLocaleAndPublishedSlug(String locale, String publishedSlug);

    Optional<PageTranslationEntity> findFirstByPublishedSlugAndPublishedSnapshotJsonIsNotNull(String publishedSlug);

    List<PageTranslationEntity> findAllByOrderByUpdatedAtDesc();

    List<PageTranslationEntity> findAllByPublishedSnapshotJsonIsNotNullOrderByLocaleAscPublishedSlugAsc();
}
