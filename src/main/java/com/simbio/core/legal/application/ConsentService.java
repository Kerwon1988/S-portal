package com.simbio.core.legal.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.LegalDocumentsReader;
import com.simbio.core.legal.api.ConsentAcceptance;
import com.simbio.core.legal.api.ConsentDocumentVersions;
import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.legal.domain.ConsentRecordEntity;
import com.simbio.core.shared.security.AuthenticatedUser;

@Service
public class ConsentService implements ConsentRecorder {

    private final ConsentRecordStore store;
    private final LegalDocumentsReader legalDocuments;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public ConsentService(ConsentRecordStore store, LegalDocumentsReader legalDocuments,
            AuditRecorder auditRecorder) {
        this(store, legalDocuments, auditRecorder, Clock.systemUTC());
    }

    ConsentService(ConsentRecordStore store, LegalDocumentsReader legalDocuments,
            AuditRecorder auditRecorder, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.legalDocuments = Objects.requireNonNull(legalDocuments);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public UUID recordAcceptance(AuthenticatedUser actor, ConsentAcceptance acceptance) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(acceptance, "acceptance must not be null");
        if (!actor.userId().equals(acceptance.subjectUserId())) {
            throw new SecurityException("Consent subject must match authenticated user");
        }
        String locale = normalizeLocale(acceptance.locale());
        if (acceptance.documentVersion() < 1) {
            throw new IllegalArgumentException("documentVersion must be positive");
        }

        String documentType = acceptance.purpose().documentType();
        var currentDocument = legalDocuments.current(documentType, locale)
                .orElseThrow(() -> new ConsentDocumentUnavailableException(documentType));
        if (currentDocument.version() != acceptance.documentVersion()) {
            throw new StaleConsentVersionException();
        }

        ConsentAcceptance normalized = new ConsentAcceptance(acceptance.subjectUserId(),
                acceptance.purpose(), locale, acceptance.documentVersion(), acceptance.source());
        Instant acceptedAt = Instant.now(clock);
        ConsentRecordEntity record = store.save(new ConsentRecordEntity(UUID.randomUUID(), normalized, acceptedAt));
        auditRecorder.record(new AuditRecord(acceptance.subjectUserId(), "LEGAL_CONSENT_ACCEPTED",
                "LEGAL_CONSENT", record.getPublicId(), null));
        return record.getPublicId();
    }

    @Override
    @Transactional
    public void recordRequiredAcceptances(AuthenticatedUser actor, String locale, ConsentSource source) {
        ConsentDocumentVersions versions = currentDocumentVersions(locale);
        recordRequiredAcceptances(actor, locale, source, versions.termsVersion(), versions.privacyVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentDocumentVersions currentDocumentVersions(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        var terms = legalDocuments.current("TERMS", normalizedLocale)
                .orElseThrow(() -> new ConsentDocumentUnavailableException("TERMS"));
        var privacy = legalDocuments.current("PRIVACY", normalizedLocale)
                .orElseThrow(() -> new ConsentDocumentUnavailableException("PRIVACY"));
        int marketingVersion = legalDocuments.current("MARKETING", normalizedLocale)
                .map(document -> document.version())
                .orElse(0);
        return new ConsentDocumentVersions(terms.version(), privacy.version(), marketingVersion);
    }

    @Override
    @Transactional
    public void recordRequiredAcceptances(
            AuthenticatedUser actor,
            String locale,
            ConsentSource source,
            int termsVersion,
            int privacyVersion) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(source, "source must not be null");
        String normalizedLocale = normalizeLocale(locale);
        ConsentDocumentVersions versions = currentDocumentVersions(normalizedLocale);
        if (versions.termsVersion() != termsVersion || versions.privacyVersion() != privacyVersion) {
            throw new StaleConsentVersionException();
        }
        recordAcceptance(actor, new ConsentAcceptance(actor.userId(), ConsentPurpose.TERMS_ACCEPTANCE,
                normalizedLocale, termsVersion, source));
        recordAcceptance(actor, new ConsentAcceptance(actor.userId(), ConsentPurpose.PRIVACY_ACKNOWLEDGEMENT,
                normalizedLocale, privacyVersion, source));
    }

    @Override
    @Transactional
    public void recordMarketingAcceptance(AuthenticatedUser actor, String locale, int marketingVersion) {
        Objects.requireNonNull(actor, "actor must not be null");
        ConsentDocumentVersions versions = currentDocumentVersions(locale);
        if (!versions.hasMarketingDocument() || versions.marketingVersion() != marketingVersion) {
            throw new StaleConsentVersionException();
        }
        recordAcceptance(actor, new ConsentAcceptance(actor.userId(), ConsentPurpose.MARKETING_COMMUNICATIONS,
                normalizeLocale(locale), marketingVersion, ConsentSource.REGISTRATION));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMarketingConsent(UUID subjectUserId) {
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        return store.existsBySubjectAndPurpose(subjectUserId, ConsentPurpose.MARKETING_COMMUNICATIONS);
    }

    private static String normalizeLocale(String locale) {
        String normalized = locale == null ? "" : locale.toLowerCase(Locale.ROOT);
        if (!normalized.equals("es") && !normalized.equals("en")) {
            throw new IllegalArgumentException("Unsupported consent locale");
        }
        return normalized;
    }
}
