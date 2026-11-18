package com.simbio.core.files.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import com.simbio.core.files.api.FilePurpose;

@Service
public class FileValidationService {

    private static final long MAX_IMAGE_BYTES = 12L * 1024 * 1024;
    private static final long MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024;
    private static final Map<String, Set<String>> EXTENSIONS = Map.of(
            "image/png", Set.of("png"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/webp", Set.of("webp"),
            "application/pdf", Set.of("pdf"));

    private final Tika tika = new Tika();

    public ValidatedFile validate(byte[] bytes, String submittedName, long declaredSize, FilePurpose purpose) {
        if (bytes == null || bytes.length == 0 || bytes.length != declaredSize || purpose == null) {
            throw new InvalidFileException();
        }
        long limit = purpose == FilePurpose.IMAGE ? MAX_IMAGE_BYTES : MAX_ATTACHMENT_BYTES;
        if (bytes.length > limit || submittedName == null || submittedName.length() > 255) {
            throw new InvalidFileException();
        }
        String extension = extensionOf(submittedName);
        String mime;
        try {
            mime = tika.detect(new ByteArrayInputStream(bytes));
        } catch (IOException exception) {
            throw new InvalidFileException();
        }
        Set<String> allowedExtensions = EXTENSIONS.get(mime);
        boolean allowedMime = allowedExtensions != null
                && (purpose != FilePurpose.IMAGE || !mime.equals("application/pdf"));
        if (!allowedMime || !allowedExtensions.contains(extension)) {
            throw new InvalidFileException();
        }
        return new ValidatedFile(bytes, mime, "upload." + extension);
    }

    private static String extensionOf(String submittedName) {
        if (submittedName.contains("/") || submittedName.contains("\\") || submittedName.contains("..")) {
            throw new InvalidFileException();
        }
        int dot = submittedName.lastIndexOf('.');
        if (dot <= 0 || dot == submittedName.length() - 1) {
            throw new InvalidFileException();
        }
        String extension = submittedName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,8}")) {
            throw new InvalidFileException();
        }
        String base = submittedName.substring(0, dot);
        if (base.contains(".")) {
            throw new InvalidFileException();
        }
        return extension;
    }

    public record ValidatedFile(byte[] bytes, String mime, String safeName) {
        public ValidatedFile {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
