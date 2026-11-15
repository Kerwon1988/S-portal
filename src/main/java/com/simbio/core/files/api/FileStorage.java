package com.simbio.core.files.api;

import java.io.InputStream;
import java.util.UUID;

public interface FileStorage {

    StoredFile store(InputStream content, String submittedName, long declaredSize, FilePurpose purpose);

    InputStream open(UUID publicId, FileAccessAuthorization authorization);
}
