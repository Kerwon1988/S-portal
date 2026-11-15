package com.simbio.core.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.identity.domain.RoleEntity;

interface RoleJpaRepository extends JpaRepository<RoleEntity, String> {
}
