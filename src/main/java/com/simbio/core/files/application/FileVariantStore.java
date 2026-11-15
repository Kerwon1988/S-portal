package com.simbio.core.files.application;

import com.simbio.core.files.domain.FileVariantEntity;

public interface FileVariantStore {

    FileVariantEntity save(FileVariantEntity variant);
}
