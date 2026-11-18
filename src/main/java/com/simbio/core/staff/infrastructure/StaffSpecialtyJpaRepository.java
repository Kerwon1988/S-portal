package com.simbio.core.staff.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.staff.domain.StaffSpecialtyEntity;

interface StaffSpecialtyJpaRepository extends JpaRepository<StaffSpecialtyEntity, Long> {

    List<StaffSpecialtyEntity> findByUserPublicIdOrderByNameAsc(UUID userPublicId);

    void deleteByUserPublicId(UUID userPublicId);
}
