package com.simbio.core.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesRandomStorageKeyOutsideWebRootAndSupportsStreamingRead() throws IOException {
        Path root = temporaryDirectory.resolve("private");
        LocalFileStorage storage = new LocalFileStorage(root);

        Path stored = storage.writeQuarantined(new ByteArrayInputStream(new byte[] {1, 2, 3}),
                "0123456789abcdef0123456789abcdef");

        assertThat(stored).startsWith(root.toAbsolutePath().normalize().resolve("quarantine"));
        assertThat(Files.readAllBytes(stored)).containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThatThrownBy(() -> storage.open("0123456789abcdef0123456789abcdef"))
                .isInstanceOf(IOException.class);
        storage.markClean("0123456789abcdef0123456789abcdef");
        try (var stream = storage.open("0123456789abcdef0123456789abcdef")) {
            assertThat(stream.readAllBytes()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        }
    }

    @Test
    void rejectsPathTraversalStorageKeys() {
        LocalFileStorage storage = new LocalFileStorage(temporaryDirectory.resolve("private"));

        assertThatThrownBy(() -> storage.open("../outside"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.remove(".."))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesAConfiguredWebRoot() {
        Path webRoot = Path.of("src", "main", "resources", "static")
                .toAbsolutePath()
                .normalize();

        assertThatThrownBy(() -> new LocalFileStorage(webRoot.resolve("uploads")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the web root");
    }
}
