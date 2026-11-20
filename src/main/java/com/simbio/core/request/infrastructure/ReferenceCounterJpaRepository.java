package com.simbio.core.request.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.simbio.core.request.domain.PublicReferenceCounterEntity;

interface ReferenceCounterJpaRepository extends JpaRepository<PublicReferenceCounterEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PublicReferenceCounterEntity c where c.businessYear = :year")
    Optional<PublicReferenceCounterEntity> findByIdForUpdate(@Param("year") int year);
}
