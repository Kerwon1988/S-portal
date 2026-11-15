package com.simbio.core.files.application;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.files.api.FileAccessAuthorization;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.domain.FileVariantEntity;

@Service
public class ImageVariantService {

    private static final long MAX_INPUT_BYTES = 12L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private static final int MAX_DIMENSION = 10_000;
    private static final List<Integer> WIDTHS = List.of(400, 900, 1600);

    private final FileStorage fileStorage;
    private final FileMetadataStore metadataStore;
    private final FileObjectStore objectStore;
    private final FileVariantStore variantStore;

    public ImageVariantService(FileStorage fileStorage, FileMetadataStore metadataStore,
            FileObjectStore objectStore, FileVariantStore variantStore) {
        this.fileStorage = fileStorage;
        this.metadataStore = metadataStore;
        this.objectStore = objectStore;
        this.variantStore = variantStore;
    }

    @Transactional
    public List<Variant> createVariants(UUID filePublicId, FileAccessAuthorization authorization) {
        byte[] source = readBounded(filePublicId, authorization);
        BufferedImage decoded = decodeBounded(source);
        List<Variant> variants = new ArrayList<>();
        for (int targetWidth : WIDTHS) {
            if (targetWidth > decoded.getWidth()) {
                if (!variants.isEmpty()) {
                    continue;
                }
                targetWidth = decoded.getWidth();
            }
            int height = Math.max(1, (int) Math.round((double) decoded.getHeight()
                    * targetWidth / decoded.getWidth()));
            BufferedImage resized = resize(decoded, targetWidth, height);
            byte[] webp = encodeWebp(resized);
            String code = "w" + targetWidth;
            String key = UUID.randomUUID().toString().replace("-", "");
            try {
                objectStore.writeQuarantined(new ByteArrayInputStream(webp), key);
                objectStore.markClean(key);
            } catch (IOException exception) {
                throw new FileStorageException(exception);
            }
            var parent = metadataStore.findByPublicId(filePublicId).orElseThrow(FileNotFoundException::new);
            variantStore.save(new FileVariantEntity(parent.getId(), code, key, targetWidth, height, "image/webp"));
            variants.add(new Variant(code, targetWidth, height, webp.length));
        }
        return List.copyOf(variants);
    }

    private byte[] readBounded(UUID publicId, FileAccessAuthorization authorization) {
        try (var input = fileStorage.open(publicId, authorization);
                var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_INPUT_BYTES) {
                    throw new InvalidFileException();
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }

    private static BufferedImage decodeBounded(byte[] source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (input == null) {
                throw new InvalidFileException();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidFileException();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new InvalidFileException();
                }
                int sample = Math.max(1, Math.max(width, height) / 1600);
                var parameter = reader.getDefaultReadParam();
                parameter.setSourceSubsampling(sample, sample, 0, 0);
                return reader.read(0, parameter);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidFileException();
        }
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
            return target;
        } finally {
            graphics.dispose();
        }
    }

    private static byte[] encodeWebp(BufferedImage image) {
        ImageIO.scanForPlugins();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IllegalStateException("The configured WebP encoder is unavailable");
        }
        ImageWriter writer = writers.next();
        try (var output = new ByteArrayOutputStream();
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), writer.getDefaultWriteParam());
            imageOutput.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        } finally {
            writer.dispose();
        }
    }

    public record Variant(String code, int width, int height, int sizeBytes) {
    }
}
