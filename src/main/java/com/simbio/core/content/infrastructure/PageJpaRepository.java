package com.simbio.core.content.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.PageEntity;

interface PageJpaRepository extends JpaRepository<PageEntity, Long> {

    Optional<PageEntity> findByPublicId(UUID publicId);

    Optional<PageEntity> findByPageKey(String pageKey);
}
