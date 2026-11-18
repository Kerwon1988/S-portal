package com.simbio.core.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.domain.AuditEntryEntity;
import com.simbio.core.shared.spi.AuditEvent;

class AuditRecorderServiceTest {

    @Test
    void recordsOnlySafeCodesAndCorrelationMetadata() {
        CapturingAuditEntryStore store = new CapturingAuditEntryStore();
        Instant now = Instant.parse("2026-11-13T18:00:00Z");
        AuditRecorderService service = new AuditRecorderService(
                store, Clock.fixed(now, ZoneOffset.UTC));

        service.record(new AuditRecord(
                UUID.randomUUID(),
                "PAGE_PUBLISHED",
                "PAGE",
                UUID.randomUUID(),
                "user@example.test"));

        assertThat(store.saved.getActionCode()).isEqualTo("PAGE_PUBLISHED");
        assertThat(store.saved.getEntityType()).isEqualTo("PAGE");
        assertThat(store.saved.getCorrelationId()).isEqualTo("REDACTED");
        assertThat(store.saved.getOccurredAt()).isEqualTo(now);
    }

    @Test
    void redactsCredentialLikeValuesInsteadOfPersistingThem() {
        CapturingAuditEntryStore store = new CapturingAuditEntryStore();
        AuditRecorderService service = new AuditRecorderService(store);

        service.record(new AuditRecord(
                null,
                "token=synthetic-secret",
                "Authorization: Bearer synthetic-secret",
                null,
                "correlation-token=synthetic-secret"));

        assertThat(store.saved.getActionCode()).isEqualTo("REDACTED");
        assertThat(store.saved.getEntityType()).isEqualTo("REDACTED");
        assertThat(store.saved.getCorrelationId()).isEqualTo("REDACTED");
    }

    @Test
    void recordsMinimalSharedAuditEventsThroughTheAuditOwner() {
        CapturingAuditEntryStore store = new CapturingAuditEntryStore();
        AuditRecorderService service = new AuditRecorderService(store);

        service.record(new AuditEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "ERROR_INVENTORY.RESOLVED",
                "ERROR_INVENTORY",
                UUID.fromString("123e4567-e89b-12d3-a456-426614174001"),
                "corr-12345678"));

        assertThat(store.saved.getActionCode()).isEqualTo("ERROR_INVENTORY.RESOLVED");
        assertThat(store.saved.getEntityType()).isEqualTo("ERROR_INVENTORY");
        assertThat(store.saved.getCorrelationId()).isEqualTo("corr-12345678");
    }

    @Test
    void retentionDeletesOnlyEntriesOlderThanThreeYears() {
        CapturingAuditEntryStore store = new CapturingAuditEntryStore();
        Instant now = Instant.parse("2026-11-13T18:00:00Z");
        AuditRecorderService service = new AuditRecorderService(store);

        service.purgeExpired(now);

        assertThat(store.deleteBefore).isEqualTo(Instant.parse("2023-11-13T18:00:00Z"));
    }

    private static final class CapturingAuditEntryStore implements AuditEntryStore {

        private AuditEntryEntity saved;
        private Instant deleteBefore;

        @Override
        public AuditEntryEntity save(AuditEntryEntity entry) {
            saved = entry;
            return entry;
        }

        @Override
        public Page<AuditEntryEntity> findAll(Pageable pageable) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        @Override
        public Page<AuditEntryEntity> search(AuditFilter filter, Pageable pageable) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        @Override
        public List<AuditEntryEntity> findByEntity(String entityType, UUID entityPublicId, Pageable pageable) {
            return List.of();
        }

        @Override
        public long deleteBefore(Instant cutoff) {
            deleteBefore = cutoff;
            return 0;
        }
    }
}
