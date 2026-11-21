package com.simbio.core.simbiosphere.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;

public interface LinkedBusinessJpaRepository extends JpaRepository<LinkedBusinessEntity, Long> {
    List<LinkedBusinessEntity> findAllByAccountLink_ClientAccountPublicId(UUID clientAccountPublicId);
}
