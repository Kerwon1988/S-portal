package com.simbio.core.simbiosphere.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;

interface LinkedBusinessJpaRepository extends JpaRepository<LinkedBusinessEntity, Long> {
}
