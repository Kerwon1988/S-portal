package com.simbio.core.staff.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.staff.domain.StaffProfileEntity;
import com.simbio.core.staff.domain.StaffSpecialtyEntity;

public interface StaffProfileStore {

    StaffProfileEntity save(StaffProfileEntity profile);

    List<StaffSpecialtyEntity> replaceSpecialties(UUID userPublicId, List<String> names);

    List<String> specialties(UUID userPublicId);

    Optional<StaffProfileEntity> find(UUID userPublicId);

    Page<StaffProfileEntity> search(String search, String status, Pageable pageable);
}
