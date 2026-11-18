package com.simbio.core.files.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.files.domain.FileVariantEntity;

interface FileVariantJpaRepository extends JpaRepository<FileVariantEntity, Long> {

    java.util.List<FileVariantEntity> findByFile_IdOrderByWidthAsc(Long fileId);
}
