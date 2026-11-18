package com.simbio.core.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.simbio.core.files.api.FilePurpose;

class FileValidationServiceTest {

    private final FileValidationService validation = new FileValidationService();

    @Test
    void acceptsImageWithMatchingSignatureAndUsesNonPersonalStorageName() {
        byte[] image = png();

        FileValidationService.ValidatedFile file =
                validation.validate(image, "customer-name.png", image.length, FilePurpose.IMAGE);

        assertThat(file.mime()).isEqualTo("image/png");
        assertThat(file.safeName()).isEqualTo("upload.png");
        assertThat(file.bytes()).containsExactly(image);
    }

    @Test
    void rejectsActiveSvgAndDoubleExtensions() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>noop()</script></svg>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validation.validate(svg, "image.svg", svg.length, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validation.validate(png(), "image.exe.png",
                png().length, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsPathTraversalAndDeclaredSizeMismatch() {
        byte[] image = png();

        assertThatThrownBy(() -> validation.validate(image, "..\\outside.png",
                image.length, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validation.validate(image, "image.png",
                image.length + 1L, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsMissingAndMismatchedExtensions() {
        byte[] image = png();

        assertThatThrownBy(() -> validation.validate(image, "image", image.length, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validation.validate(image, "image.gif", image.length, FilePurpose.IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsMissingFilePurpose() {
        byte[] image = png();

        assertThatThrownBy(() -> validation.validate(image, "image.png", image.length, null))
                .isInstanceOf(InvalidFileException.class);
    }

    private static byte[] png() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lI0AAAAASUVORK5CYII=");
    }
}
