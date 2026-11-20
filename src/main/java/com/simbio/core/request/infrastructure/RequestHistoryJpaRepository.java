package com.simbio.core.request.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.request.domain.RequestStatusHistoryEntity;

interface RequestHistoryJpaRepository extends JpaRepository<RequestStatusHistoryEntity, Long> {
    List<RequestStatusHistoryEntity> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);
}
