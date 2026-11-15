package com.simbio.core.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.audit.domain.AuditEntryEntity;

@Service
public class AuditRecorderService implements AuditRecorder {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9_.-]{0,63}");
    private static final Pattern EMAIL = Pattern.compile("(?i)[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}");
    private static final Pattern TOKEN = Pattern.compile("(?i)(token|secret|password|authorization)\\s*[:=]\\s*\\S+");

    private final AuditEntryStore store;
    private final Clock clock;

    public AuditRecorderService(AuditEntryStore store) {
        this(store, Clock.systemUTC());
    }

    AuditRecorderService(AuditEntryStore store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void record(AuditRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        store.save(new AuditEntryEntity(
                record.actorId(),
                safeCode(record.actionCode()),
                safeCode(record.entityType()),
                record.entityPublicId(),
                Instant.now(clock),
                safeCorrelationId(record.correlationId())));
    }

    public AuditPage page(AuditFilter filter, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("occurredAt").descending());
        var result = store.search(filter, pageable);
        return new AuditPage(result.getContent().stream().map(AuditView::from).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    public java.util.List<AuditView> forEntity(String entityType, UUID entityPublicId) {
        return store.findByEntity(safeCode(entityType), Objects.requireNonNull(entityPublicId),
                        org.springframework.data.domain.PageRequest.of(0, 100,
                                org.springframework.data.domain.Sort.by("occurredAt").descending()))
                .stream().map(AuditView::from).toList();
    }

    public long purgeExpired(Instant now) {
        return store.deleteBefore(Objects.requireNonNull(now).minus(java.time.Duration.ofDays(365L * 3)));
    }

    private static String safeCode(String value) {
        if (value == null || EMAIL.matcher(value).find() || TOKEN.matcher(value).find()
                || !CODE.matcher(value).matches()) {
            return "REDACTED";
        }
        return value;
    }

    private static String safeCorrelationId(String value) {
        if (value == null || value.length() > 64 || EMAIL.matcher(value).find() || TOKEN.matcher(value).find()) {
            return "REDACTED";
        }
        return value.replaceAll("[^A-Za-z0-9_-]", "");
    }
}
