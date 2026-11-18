package com.simbio.core.staff.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.staff.application.StaffProfileStore;
import com.simbio.core.staff.domain.StaffProfileEntity;
import com.simbio.core.staff.domain.StaffSpecialtyEntity;

@Repository
class JpaStaffProfileStore implements StaffProfileStore {

    private final StaffProfileJpaRepository profiles;
    private final StaffSpecialtyJpaRepository specialties;

    JpaStaffProfileStore(StaffProfileJpaRepository profiles, StaffSpecialtyJpaRepository specialties) {
        this.profiles = profiles;
        this.specialties = specialties;
    }

    @Override
    @Transactional
    public StaffProfileEntity save(StaffProfileEntity profile) {
        return profiles.save(profile);
    }

    @Override
    @Transactional
    public List<StaffSpecialtyEntity> replaceSpecialties(UUID userPublicId, List<String> names) {
        specialties.deleteByUserPublicId(userPublicId);
        return specialties.saveAll(names.stream().map(name -> new StaffSpecialtyEntity(userPublicId, name)).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> specialties(UUID userPublicId) {
        return specialties.findByUserPublicIdOrderByNameAsc(userPublicId).stream()
                .map(StaffSpecialtyEntity::getName).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffProfileEntity> find(UUID userPublicId) {
        return profiles.findById(userPublicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffProfileEntity> search(String search, String status, Pageable pageable) {
        return profiles.search(search, status, pageable);
    }
}
