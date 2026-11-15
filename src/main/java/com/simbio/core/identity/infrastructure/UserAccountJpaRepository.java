package com.simbio.core.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.identity.domain.UserAccountEntity;

interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByEmailNormalized(String normalizedEmail);

    boolean existsByRoles_Code(String code);

    Optional<UserAccountEntity> findByPublicId(UUID publicId);
}
