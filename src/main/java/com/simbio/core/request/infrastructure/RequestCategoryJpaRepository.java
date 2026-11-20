package com.simbio.core.request.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.request.domain.RequestCategoryEntity;

interface RequestCategoryJpaRepository extends JpaRepository<RequestCategoryEntity, String> {
    boolean existsByCodeAndActiveTrue(String code);
}
