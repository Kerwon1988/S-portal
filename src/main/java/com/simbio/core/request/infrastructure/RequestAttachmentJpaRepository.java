package com.simbio.core.request.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.request.domain.RequestAttachmentEntity;

interface RequestAttachmentJpaRepository extends JpaRepository<RequestAttachmentEntity, Long> {
    List<RequestAttachmentEntity> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);
    Optional<RequestAttachmentEntity> findByRequestIdAndFilePublicId(Long requestId, UUID filePublicId);
    long countByRequestId(Long requestId);
}
