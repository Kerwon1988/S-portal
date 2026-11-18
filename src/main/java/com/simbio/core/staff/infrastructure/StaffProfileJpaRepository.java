package com.simbio.core.staff.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.staff.domain.StaffProfileEntity;

interface StaffProfileJpaRepository extends JpaRepository<StaffProfileEntity, java.util.UUID> {

    @Query("""
            select distinct profile from StaffProfileEntity profile
            left join StaffSpecialtyEntity specialty on specialty.userPublicId = profile.userPublicId
            where (:status is null or profile.status = :status)
              and (:search is null or lower(coalesce(profile.jobTitle, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(specialty.name, '')) like lower(concat('%', :search, '%')))
            """)
    Page<StaffProfileEntity> search(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);
}
