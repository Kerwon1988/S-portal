package com.simbio.core.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.identity.domain.PermissionEntity;

interface PermissionJpaRepository extends JpaRepository<PermissionEntity, String> {
}
