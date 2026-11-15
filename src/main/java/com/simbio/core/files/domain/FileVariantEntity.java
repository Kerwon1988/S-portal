package com.simbio.core.files.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "file_variant", schema = "files",
        uniqueConstraints = @UniqueConstraint(name = "uq_file_variant_code", columnNames = {"file_id", "variant_code"}))
public class FileVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_id", nullable = false)
    private Long fileId;
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

    protected FileVariantEntity() {
    }

    public FileVariantEntity(Long fileId, String variantCode, String storageKey, int width, int height, String mime) {
        this.fileId = fileId;
        this.variantCode = variantCode;
        this.storageKey = storageKey;
        this.width = width;
        this.height = height;
        this.mime = mime;
    }
}
