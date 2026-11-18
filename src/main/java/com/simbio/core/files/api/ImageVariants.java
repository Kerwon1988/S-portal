package com.simbio.core.files.api;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface ImageVariants {

    List<ImageVariant> createVariants(UUID filePublicId, FileAccessAuthorization authorization);

    List<ImageVariant> listVariants(UUID filePublicId, FileAccessAuthorization authorization);

    InputStream openVariant(UUID filePublicId, String variantCode, FileAccessAuthorization authorization);
}
