package com.simbio.core.files.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileObjectStore {

    Path writeQuarantined(InputStream content, String storageKey) throws IOException;

    void markClean(String storageKey) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void remove(String storageKey) throws IOException;
}
