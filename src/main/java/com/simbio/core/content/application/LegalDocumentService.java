package com.simbio.core.content.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.LegalDocumentsReader;
import com.simbio.core.content.api.PublishedLegalDocument;
import com.simbio.core.content.domain.LegalDocumentVersionEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalDocumentService implements LegalDocumentsReader {

    private static final List<String> TYPES = List.of("NOTICE", "PRIVACY", "COOKIES", "TERMS", "MARKETING");
    private static final ZoneId EFFECTIVE_DATE_ZONE = ZoneId.of("Europe/Madrid");
    private final ContentStore store;
    private final ContentSanitizer sanitizer;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public LegalDocumentService(ContentStore store, ContentSanitizer sanitizer, AuditRecorder auditRecorder) {
        this(store, sanitizer, auditRecorder, Clock.systemUTC());
    }

    LegalDocumentService(ContentStore store, ContentSanitizer sanitizer, AuditRecorder auditRecorder, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.sanitizer = Objects.requireNonNull(sanitizer);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public LegalDocumentVersionEntity saveDraft(AuthenticatedUser actor, String type, String locale,
            String title, String markdown, LocalDate effectiveDate) {
        requireEditor(actor);
        String normalizedType = normalizeType(type);
        String normalizedLocale = normalizeLocale(locale);
        String safeTitle = required(title, 200);
        if (markdown == null || markdown.length() > 16000) {
            throw new ContentValidationException("Invalid legal text");
        }
        sanitizer.sanitize(markdown);
        int nextVersion = store.findLatestLegalDocument(normalizedType, normalizedLocale)
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);
        Instant now = Instant.now(clock);
        Instant effectiveFrom = effectiveDate == null ? null
                : effectiveDate.atStartOfDay(EFFECTIVE_DATE_ZONE).toInstant();
        LegalDocumentVersionEntity draft = new LegalDocumentVersionEntity(UUID.randomUUID(), normalizedType,
                normalizedLocale, nextVersion, safeTitle, markdown, "PENDING", effectiveFrom, actor.userId(), now);
        store.saveLegalDocument(draft);
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_LEGAL_DRAFT_SAVED",
                "CONTENT_LEGAL_DOCUMENT", draft.getPublicId(), null));
        return draft;
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentVersionEntity> drafts(AuthenticatedUser actor) {
        requireEditor(actor);
        return store.findLegalDocuments();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<PublishedLegalDocument> current(String type, String locale) {
        String normalizedType = normalizeType(type);
        String normalizedLocale = normalizeLocale(locale);
        return store.findCurrentLegalDocument(normalizedType, normalizedLocale, Instant.now(clock))
                .map(document -> new PublishedLegalDocument(document.getDocumentType(), document.getLocale(),
                        document.getVersion(), document.getTitle(), sanitizer.sanitize(document.getContentMarkdown()),
                        document.getEffectiveFrom()));
    }

    public static String typeForSlug(String slug) {
        return switch (slug) {
            case "aviso-legal", "legal-notice" -> "NOTICE";
            case "privacidad", "privacy" -> "PRIVACY";
            case "cookies" -> "COOKIES";
            case "terminos", "terms" -> "TERMS";
            default -> throw new ContentNotFoundException();
        };
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "" : type.toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) {
            throw new ContentValidationException("Unsupported legal document type");
        }
        return normalized;
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || !(locale.equals("es") || locale.equals("en"))) {
            throw new ContentValidationException("Only es and en locales are supported");
        }
        return locale;
    }

    private static String required(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ContentValidationException("Invalid legal document title");
        }
        return value.trim();
    }

    private static void requireEditor(AuthenticatedUser actor) {
        if (actor == null || !actor.hasPermission(Permission.CONTENT_EDIT)) {
            throw new ContentPermissionException();
        }
    }
}
