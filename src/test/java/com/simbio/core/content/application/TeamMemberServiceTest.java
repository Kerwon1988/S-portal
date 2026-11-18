package com.simbio.core.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.SanitizedHtml;
import com.simbio.core.content.domain.TeamMemberEntity;
import com.simbio.core.files.api.CleanFileMetadata;
import com.simbio.core.files.api.CleanFileMetadataReader;
import com.simbio.core.files.api.ImageVariant;
import com.simbio.core.files.api.ImageVariants;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamMemberServiceTest {

    private final ContentStore store = mock(ContentStore.class);
    private final ContentSanitizer sanitizer = mock(ContentSanitizer.class);
    private final CleanFileMetadataReader cleanFileMetadataReader = mock(CleanFileMetadataReader.class);
    private final ImageVariants imageVariants = mock(ImageVariants.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final AuthenticatedUser editor = new AuthenticatedUser(
            UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT));
    private final TeamMemberService service = new TeamMemberService(
            store, sanitizer, cleanFileMetadataReader, imageVariants, auditRecorder);

    @BeforeEach
    void setUp() {
        when(sanitizer.sanitize(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation ->
                new SanitizedHtml(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("TC-CMS-009: team material cannot be self-approved or published")
    void alwaysSavesTeamMaterialAsUnpublishedDraft() {
        TeamMemberDraftView draft = service.save(editor, null, "es", 0, "Synthetic name", "Role",
                "Synthetic biography", null, "", null);

        assertThat(draft.active()).isFalse();
        assertThat(draft.materialsApproved()).isFalse();
        verify(store).saveTeamMember(org.mockito.ArgumentMatchers.any());
        verify(store, never()).findActiveTeamMembers();
    }

    @Test
    @DisplayName("TC-CMS-009: unsafe profile links are rejected")
    void rejectsNonHttpsProfileLinks() {
        assertThatThrownBy(() -> service.save(editor, null, "es", 0, "Synthetic name", "Role",
                "Synthetic biography", null, "", "http://example.test/profile"))
                .isInstanceOf(ContentValidationException.class);
    }

    @Test
    @DisplayName("TC-CMS-009: public team profiles require clean referenced photos")
    void omitsMembersWhosePhotoIsNotClean() {
        UUID fileId = UUID.randomUUID();
        TeamMemberEntity member = memberWithBothLocales(fileId);
        when(store.findActiveTeamMembers()).thenReturn(List.of(member));
        when(cleanFileMetadataReader.findClean(fileId)).thenReturn(Optional.empty());

        assertThat(service.publicMembers("es")).isEmpty();
    }

    @Test
    @DisplayName("TC-CMS-009: public profiles include localized alt and sanitized biography")
    void rendersCleanPhotoWithLocalizedAlternativeText() {
        UUID fileId = UUID.randomUUID();
        TeamMemberEntity member = memberWithBothLocales(fileId);
        when(store.findActiveTeamMembers()).thenReturn(List.of(member));
        when(cleanFileMetadataReader.findClean(fileId))
                .thenReturn(Optional.of(new CleanFileMetadata(fileId, "image/webp", 128)));
        when(imageVariants.listVariants(org.mockito.ArgumentMatchers.eq(fileId),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new ImageVariant("w400", 400, 300, "image/webp", 128)));

        var published = service.publicMembers("en").getFirst();

        assertThat(published.photoUrl()).isEqualTo("/public/media/" + fileId);
        assertThat(published.photoSrcSet()).contains("w400 400w");
        assertThat(published.photoAlt()).isEqualTo("English portrait");
        assertThat(published.bio().value()).contains("Synthetic English biography");
    }

    private static TeamMemberEntity memberWithBothLocales(UUID fileId) {
        TeamMemberEntity member = new TeamMemberEntity(UUID.randomUUID(), 0, true, true);
        member.translation("es").edit("Nombre sintético", "Rol", "Biografía sintética",
                fileId, "Retrato", null);
        member.translation("en").edit("Synthetic name", "Role", "Synthetic English biography",
                fileId, "English portrait", null);
        return member;
    }
}
