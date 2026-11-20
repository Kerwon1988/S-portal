package com.simbio.core.request.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.request.domain.RequestAssignmentEntity;

interface RequestAssignmentJpaRepository extends JpaRepository<RequestAssignmentEntity, Long> {
    List<RequestAssignmentEntity> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);
    boolean existsByRequestIdAndStaffUserId(Long requestId, UUID staffUserId);
    void deleteByRequestId(Long requestId);
}
