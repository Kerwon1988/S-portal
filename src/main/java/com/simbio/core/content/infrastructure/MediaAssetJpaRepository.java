package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.MediaAssetEntity;

interface MediaAssetJpaRepository extends JpaRepository<MediaAssetEntity, UUID> {

    Optional<MediaAssetEntity> findByFilePublicId(UUID filePublicId);

    List<MediaAssetEntity> findAllByOrderByCreatedAtDesc();
}
