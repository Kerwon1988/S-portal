package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.PageVersionEntity;

interface PageVersionJpaRepository extends JpaRepository<PageVersionEntity, Long> {

    List<PageVersionEntity> findByTranslationPublicIdOrderByVersionDesc(UUID translationPublicId);

    Optional<PageVersionEntity> findByTranslationPublicIdAndVersion(UUID translationPublicId, int version);
}
