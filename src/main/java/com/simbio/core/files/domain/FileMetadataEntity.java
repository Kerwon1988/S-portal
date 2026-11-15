package com.simbio.core.files.domain;

import java.time.Instant;
import java.util.UUID;

import com.simbio.core.files.api.ScanState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "file_metadata", schema = "files")
public class FileMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "storage_key", nullable = false, unique = true, length = 160)
    private String storageKey;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(name = "detected_mime", nullable = false, length = 127)
    private String detectedMime;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_state", nullable = false, length = 16)
    private ScanState scanState;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FileMetadataEntity() {
    }

    public FileMetadataEntity(UUID publicId, String storageKey, String originalName, String detectedMime,
            long sizeBytes, ScanState scanState, Instant createdAt) {
        this.publicId = publicId;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.detectedMime = detectedMime;
        this.sizeBytes = sizeBytes;
        this.scanState = scanState;
        this.createdAt = createdAt;
    }

    public void setScanState(ScanState scanState) {
        this.scanState = scanState;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Long getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getDetectedMime() {
        return detectedMime;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public ScanState getScanState() {
        return scanState;
    }
}
