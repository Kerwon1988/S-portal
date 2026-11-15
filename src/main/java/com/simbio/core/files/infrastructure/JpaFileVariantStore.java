package com.simbio.core.files.infrastructure;

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
}
