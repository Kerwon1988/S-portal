package com.simbio.core.legal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.LegalDocumentsReader;
import com.simbio.core.content.api.PublishedLegalDocument;
import com.simbio.core.content.api.SanitizedHtml;
import com.simbio.core.legal.api.ConsentAcceptance;
import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.legal.domain.ConsentRecordEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;

class ConsentServiceTest {

    private static final Instant ACCEPTED_AT = Instant.parse("2026-11-18T08:00:00Z");
    private final ConsentRecordStore store = org.mockito.Mockito.mock(ConsentRecordStore.class);
    private final LegalDocumentsReader documents = org.mockito.Mockito.mock(LegalDocumentsReader.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ConsentService service = new ConsentService(store, documents, auditRecorder,
            Clock.fixed(ACCEPTED_AT, ZoneOffset.UTC));

    @Test
    void recordsOnlyTheCurrentPublishedVersionAndServerTime() {
        UUID subjectId = UUID.randomUUID();
        when(documents.current("TERMS", "es")).thenReturn(Optional.of(approved("TERMS", "es", 3)));
        when(store.save(any(ConsentRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticatedUser actor = user(subjectId);
        UUID recordId = service.recordAcceptance(actor, new ConsentAcceptance(subjectId,
                ConsentPurpose.TERMS_ACCEPTANCE, "ES", 3, ConsentSource.REGISTRATION));

        org.mockito.ArgumentCaptor<ConsentRecordEntity> saved =
                org.mockito.ArgumentCaptor.forClass(ConsentRecordEntity.class);
        verify(store).save(saved.capture());
        assertThat(recordId).isEqualTo(saved.getValue().getPublicId());
        assertThat(saved.getValue().getSubjectUserId()).isEqualTo(subjectId);
        assertThat(saved.getValue().getPurposeCode()).isEqualTo("TERMS_ACCEPTANCE");
        assertThat(saved.getValue().getDocumentType()).isEqualTo("TERMS");
        assertThat(saved.getValue().getDocumentLocale()).isEqualTo("es");
        assertThat(saved.getValue().getDocumentVersion()).isEqualTo(3);
        assertThat(saved.getValue().getAcceptedAt()).isEqualTo(ACCEPTED_AT);
        assertThat(saved.getValue().getSourceCode()).isEqualTo("REGISTRATION");
        verify(auditRecorder).record(any(AuditRecord.class));
    }

    @Test
    void rejectsStaleOrUnavailableLegalVersionsWithoutPersisting() {
        UUID subjectId = UUID.randomUUID();
        when(documents.current("PRIVACY", "en")).thenReturn(Optional.of(approved("PRIVACY", "en", 4)));
        ConsentAcceptance stale = new ConsentAcceptance(subjectId,
                ConsentPurpose.PRIVACY_ACKNOWLEDGEMENT, "en", 3, ConsentSource.REGISTRATION);

        AuthenticatedUser actor = user(subjectId);
        assertThatThrownBy(() -> service.recordAcceptance(actor, stale))
                .isInstanceOf(StaleConsentVersionException.class);

        when(documents.current("TERMS", "en")).thenReturn(Optional.empty());
        ConsentAcceptance unavailable = new ConsentAcceptance(subjectId,
                ConsentPurpose.TERMS_ACCEPTANCE, "en", 1, ConsentSource.REGISTRATION);
        assertThatThrownBy(() -> service.recordAcceptance(actor, unavailable))
                .isInstanceOf(ConsentDocumentUnavailableException.class);
        verify(store, never()).save(any(ConsentRecordEntity.class));
        verify(auditRecorder, never()).record(any(AuditRecord.class));
    }

    @Test
    void rejectsConsentForAnotherUserBeforeReadingDocuments() {
        UUID actorId = UUID.randomUUID();
        ConsentAcceptance acceptance = new ConsentAcceptance(UUID.randomUUID(),
                ConsentPurpose.TERMS_ACCEPTANCE, "es", 1, ConsentSource.REGISTRATION);

        assertThatThrownBy(() -> service.recordAcceptance(user(actorId), acceptance))
                .isInstanceOf(SecurityException.class);

        verify(documents, never()).current(any(), any());
        verify(store, never()).save(any(ConsentRecordEntity.class));
        verify(auditRecorder, never()).record(any(AuditRecord.class));
    }

    @Test
    void omittedMarketingAcceptanceRemainsFalse() {
        UUID subjectId = UUID.randomUUID();
        when(store.existsBySubjectAndPurpose(subjectId, ConsentPurpose.MARKETING_COMMUNICATIONS))
                .thenReturn(false);

        assertThat(service.hasMarketingConsent(subjectId)).isFalse();
    }

    @Test
    void recordsRequiredTermsAndPrivacyAsSeparateCurrentAcceptances() {
        UUID subjectId = UUID.randomUUID();
        AuthenticatedUser actor = user(subjectId);
        when(documents.current("TERMS", "es")).thenReturn(Optional.of(approved("TERMS", "es", 3)));
        when(documents.current("PRIVACY", "es")).thenReturn(Optional.of(approved("PRIVACY", "es", 7)));
        when(store.save(any(ConsentRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordRequiredAcceptances(actor, "es", ConsentSource.ADMIN_INVITATION);

        org.mockito.ArgumentCaptor<ConsentRecordEntity> saved =
                org.mockito.ArgumentCaptor.forClass(ConsentRecordEntity.class);
        verify(store, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ConsentRecordEntity::getPurposeCode)
                .containsExactly("TERMS_ACCEPTANCE", "PRIVACY_ACKNOWLEDGEMENT");
        assertThat(saved.getAllValues()).extracting(ConsentRecordEntity::getDocumentVersion)
                .containsExactly(3, 7);
        assertThat(saved.getAllValues()).allSatisfy(record -> {
            assertThat(record.getSubjectUserId()).isEqualTo(subjectId);
            assertThat(record.getSourceCode()).isEqualTo("ADMIN_INVITATION");
        });
        verify(auditRecorder, times(2)).record(any(AuditRecord.class));
    }

    private static PublishedLegalDocument approved(String type, String locale, int version) {
        return new PublishedLegalDocument(type, locale, version, "Approved",
                new SanitizedHtml("Approved"), Instant.EPOCH);
    }

    private static AuthenticatedUser user(UUID userId) {
        return new AuthenticatedUser(userId, Role.CLIENT, java.util.Set.of());
    }
}
