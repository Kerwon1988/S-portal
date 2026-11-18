package com.simbio.core.files.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "file_variant", schema = "files",
        uniqueConstraints = @UniqueConstraint(name = "uq_file_variant_code", columnNames = {"file_id", "variant_code"}))
public class FileVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_file_variant_file"))
    private FileMetadataEntity file;
    @Column(name = "variant_code", nullable = false, length = 32)
    private String variantCode;
    @Column(name = "storage_key", nullable = false, unique = true, length = 160)
    private String storageKey;
    @Column(nullable = false)
    private int width;
    @Column(nullable = false)
    private int height;
    @Column(nullable = false, length = 127)
    private String mime;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected FileVariantEntity() {
    }

    public FileVariantEntity(
            FileMetadataEntity file, String variantCode, String storageKey, int width, int height,
            String mime, long sizeBytes) {
        this.file = java.util.Objects.requireNonNull(file);
        this.variantCode = variantCode;
        this.storageKey = storageKey;
        this.width = width;
        this.height = height;
        this.mime = mime;
        this.sizeBytes = sizeBytes;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public Long getFileId() {
        return file.getId();
    }

    public String getStorageKey() {
        return storageKey;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getMime() {
        return mime;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
