package com.simbio.core.files.application;

import java.util.List;

import com.simbio.core.files.domain.FileVariantEntity;

public interface FileVariantStore {

    FileVariantEntity save(FileVariantEntity variant);

    List<FileVariantEntity> findByFileId(Long fileId);
}
