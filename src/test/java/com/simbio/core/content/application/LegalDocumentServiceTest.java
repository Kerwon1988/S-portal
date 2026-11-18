package com.simbio.core.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.SanitizedHtml;
import com.simbio.core.content.domain.LegalDocumentVersionEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegalDocumentServiceTest {

    private final ContentStore store = mock(ContentStore.class);
    private final ContentSanitizer sanitizer = mock(ContentSanitizer.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final Instant now = Instant.parse("2026-11-17T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AuthenticatedUser editor = new AuthenticatedUser(
            UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT));
    private final LegalDocumentService service = new LegalDocumentService(store, sanitizer, auditRecorder, clock);

    @BeforeEach
    void setUp() {
        when(sanitizer.sanitize(anyString())).thenAnswer(invocation ->
                new SanitizedHtml(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("TC-CMS-010: legal drafts version and keep the proposed effective date pending")
    void savesVersionedPendingDraftWithMadridEffectiveDate() {
        LegalDocumentVersionEntity previous = new LegalDocumentVersionEntity(
                UUID.randomUUID(), "TERMS", "es", 3, "Previous terms", "Old copy",
                "PENDING", null, editor.userId(), now.minusSeconds(60));
        when(store.findLatestLegalDocument("TERMS", "es")).thenReturn(Optional.of(previous));
        when(store.saveLegalDocument(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate effectiveDate = LocalDate.of(2030, 1, 2);

        LegalDocumentVersionEntity draft = service.saveDraft(
                editor, "terms", "es", "Terms", "Synthetic legal copy", effectiveDate);

        assertThat(draft.getVersion()).isEqualTo(4);
        assertThat(draft.getApprovalState()).isEqualTo("PENDING");
        assertThat(draft.getEffectiveFrom())
                .isEqualTo(effectiveDate.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        assertThat(draft.getActorId()).isEqualTo(editor.userId());
        verify(auditRecorder).record(any());
    }

    @Test
    @DisplayName("TC-CMS-010: legal draft editing requires CONTENT_EDIT before store access")
    void rejectsUnprivilegedLegalDraftChangesWithoutStoreAccess() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.saveDraft(
                employee, "TERMS", "es", "Terms", "Synthetic legal copy", null))
                .isInstanceOf(ContentPermissionException.class);

        verify(store, never()).findLatestLegalDocument(anyString(), anyString());
        verify(store, never()).saveLegalDocument(any());
        verify(auditRecorder, never()).record(any());
    }
}
