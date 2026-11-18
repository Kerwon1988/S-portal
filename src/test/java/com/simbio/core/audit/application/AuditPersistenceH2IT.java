package com.simbio.core.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.audit.domain.AuditEntryEntity;

@SpringBootTest
@ActiveProfiles("test")
class AuditPersistenceH2IT {

    @Autowired
    private AuditRecorder auditRecorder;

    @Autowired
    private AuditRecorderService auditService;

    @Autowired
    private AuditEntryStore auditEntryStore;

    @Test
    void persistsMinimalAuditFieldsAndSearchesByActorActionAndTime() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant before = Instant.now().minusSeconds(1);
        auditRecorder.record(new AuditRecord(actorId, "CMS_PAGE_PUBLISHED", "PAGE", entityId, "synthetic-correlation"));

        AuditPage page = auditService.page(new AuditFilter(actorId, "CMS_PAGE_PUBLISHED", before, null), 0, 25);

        assertThat(page.entries()).anySatisfy(entry -> {
            assertThat(entry.actorId()).isEqualTo(actorId);
            assertThat(entry.actionCode()).isEqualTo("CMS_PAGE_PUBLISHED");
            assertThat(entry.entityPublicId()).isEqualTo(entityId);
            assertThat(entry.correlationId()).isEqualTo("synthetic-correlation");
        });
    }

    @Test
    void retentionDeletesEntriesBeyondThreeYearsAndKeepsRecentEntries() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiredAt = now.atZone(java.time.ZoneOffset.UTC).minusYears(3).minusSeconds(1).toInstant();
        UUID expiredActor = UUID.randomUUID();
        UUID currentActor = UUID.randomUUID();
        auditEntryStore.save(new AuditEntryEntity(
                expiredActor, "RETENTION_EXPIRED", "TEST", UUID.randomUUID(), expiredAt, "old-correlation"));
        auditEntryStore.save(new AuditEntryEntity(
                currentActor, "RETENTION_CURRENT", "TEST", UUID.randomUUID(), now.minusSeconds(1), "new-correlation"));

        assertThat(auditService.purgeExpired(now)).isGreaterThanOrEqualTo(1);
        assertThat(auditService.page(
                new AuditFilter(expiredActor, "RETENTION_EXPIRED", expiredAt, null), 0, 25).entries()).isEmpty();
        assertThat(auditService.page(
                new AuditFilter(currentActor, "RETENTION_CURRENT", now.minusSeconds(10), null), 0, 25).entries())
                .hasSize(1);
    }
}
