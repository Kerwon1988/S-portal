package com.simbio.core.content.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.SlugRedirectEntity;

interface SlugRedirectJpaRepository extends JpaRepository<SlugRedirectEntity, Long> {

    Optional<SlugRedirectEntity> findByLocaleAndOldSlug(String locale, String oldSlug);
}
