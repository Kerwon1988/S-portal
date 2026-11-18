package com.simbio.core.files.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.application.FileVariantStore;
import com.simbio.core.files.domain.FileVariantEntity;

@Repository
class JpaFileVariantStore implements FileVariantStore {

    private final FileVariantJpaRepository repository;

    JpaFileVariantStore(FileVariantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public FileVariantEntity save(FileVariantEntity variant) {
        return repository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVariantEntity> findByFileId(Long fileId) {
        return repository.findByFile_IdOrderByWidthAsc(fileId);
    }
}
