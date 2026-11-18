package com.simbio.core.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.PagePublished;
import com.simbio.core.content.domain.ApprovalState;
import com.simbio.core.content.domain.ContentBlockEntity;
import com.simbio.core.content.domain.ContentBlockType;
import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.MediaAssetEntity;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.files.api.CleanFileMetadata;
import com.simbio.core.files.api.CleanFileMetadataReader;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ImageVariant;
import com.simbio.core.files.api.ImageVariants;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ContentServiceTest {

    private ContentStore store;
    private ContentSanitizer sanitizer;
    private FileStorage fileStorage;
    private CleanFileMetadataReader cleanFileMetadataReader;
    private ImageVariants imageVariants;
    private AuditRecorder auditRecorder;
    private ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
    private ContentService service;
    private final AuthenticatedUser editor = new AuthenticatedUser(
            UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT));

    @BeforeEach
    void setUp() {
        store = mock(ContentStore.class);
        sanitizer = mock(ContentSanitizer.class);
        fileStorage = mock(FileStorage.class);
        cleanFileMetadataReader = mock(CleanFileMetadataReader.class);
        imageVariants = mock(ImageVariants.class);
        auditRecorder = mock(AuditRecorder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ContentService(store, sanitizer, fileStorage, cleanFileMetadataReader,
                imageVariants, auditRecorder,
                eventPublisher, new ObjectMapper(), clock);
        when(sanitizer.sanitize(anyString())).thenAnswer(invocation ->
                new com.simbio.core.content.api.SanitizedHtml(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("TC-CMS-002: commands without CONTENT_EDIT do not access the store")
    void rejectsCommandsWithoutContentEditBeforeAccessingContent() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.saveDraft(employee, null))
                .isInstanceOf(ContentPermissionException.class);

        verify(store, never()).findPageByKey(anyString());
    }

    @Test
    @DisplayName("TC-CMS-013: legal and commercial copy cannot publish before approval")
    void publicationRequiresApprovalForLegalAndCommercialCopy() {
        PageEntity page = pageWithTranslation(ContentPageType.REQUEST_CONDITIONS, ApprovalState.PENDING);
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> service.publish(editor, page.getPublicId(), "es"))
                .isInstanceOf(ContentApprovalRequiredException.class);

        verify(store, never()).saveTranslation(any());
        verify(auditRecorder, never()).record(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("TC-CMS-002: publication versions, audits, invalidates and emits IDs")
    void publicationStoresImmutableSnapshotAuditsAndPublishesOnlyIdentifiers() {
        PageEntity page = pageWithTranslation(ContentPageType.PAGE, ApprovalState.APPROVED);
        PageTranslationEntity translation = page.getTranslations().getFirst();
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findPublishedByLocaleAndSlug("es", "about"))
                .thenReturn(Optional.empty());
        when(store.saveTranslation(translation)).thenReturn(translation);
        when(store.saveVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PagePublished published = service.publish(editor, page.getPublicId(), "es");

        assertThat(translation.getPublishedSlug()).isEqualTo("about");
        assertThat(translation.getPublishedSnapshotJson()).contains("\"title\":\"About\"");
        verify(store).saveVersion(any());
        verify(auditRecorder).record(any());
        verify(eventPublisher).publishEvent(published);
    }

    @Test
    @DisplayName("TC-CMS-008: quarantined media cannot enter the content library")
    void quarantineMediaNeverEntersContentLibraryOrImageVariantPipeline() {
        UUID fileId = UUID.randomUUID();
        when(fileStorage.store(any(), anyString(), anyLong(), any(FilePurpose.class)))
                .thenReturn(new StoredFile(fileId, "image/png", 16, ScanState.QUARANTINED));

        assertThatThrownBy(() -> service.storeMedia(editor, new ByteArrayInputStream(new byte[16]),
                "photo.png", 16, "Imagen", "Image"))
                .isInstanceOf(ContentMediaNotCleanException.class);

        verify(store, never()).saveMedia(any());
        verify(imageVariants, never()).createVariants(any(), any());
    }

    @Test
    @DisplayName("TC-CMS-008: clean media retains content alt while Files owns file metadata")
    void cleanMediaKeepsOnlyContentAltTextAndUsesFilesVariantPipeline() {
        UUID fileId = UUID.randomUUID();
        MediaAssetEntity saved = new MediaAssetEntity(fileId, "Imagen en español", "English image", Instant.now(clock));
        when(fileStorage.store(any(), anyString(), anyLong(), any(FilePurpose.class)))
                .thenReturn(new StoredFile(fileId, "image/png", 16, ScanState.CLEAN));
        when(imageVariants.createVariants(eq(fileId), any())).thenReturn(java.util.List.of());
        when(store.saveMedia(any())).thenReturn(saved);

        MediaAssetEntity media = service.storeMedia(editor, new ByteArrayInputStream(new byte[16]),
                "photo.png", 16, "Imagen en español", "English image");

        assertThat(media.getFilePublicId()).isEqualTo(fileId);
        assertThat(media.altFor("es")).isEqualTo("Imagen en español");
        assertThat(media.altFor("en")).isEqualTo("English image");
        verify(imageVariants).createVariants(eq(fileId), any());
        verify(store).saveMedia(any());
    }

    @Test
    @DisplayName("TC-CMS-008: the media selector exposes only clean registered files")
    void mediaLibraryOmitsFilesWithoutCleanMetadata() {
        UUID cleanId = UUID.randomUUID();
        UUID quarantinedId = UUID.randomUUID();
        MediaAssetEntity cleanMedia = new MediaAssetEntity(cleanId, "Imagen", "Image", Instant.now(clock));
        MediaAssetEntity quarantinedMedia = new MediaAssetEntity(
                quarantinedId, "Pendiente", "Pending", Instant.now(clock));
        when(store.findAllMedia()).thenReturn(java.util.List.of(cleanMedia, quarantinedMedia));
        when(cleanFileMetadataReader.findClean(cleanId))
                .thenReturn(Optional.of(new CleanFileMetadata(cleanId, "image/webp", 128)));
        when(cleanFileMetadataReader.findClean(quarantinedId)).thenReturn(Optional.empty());

        assertThat(service.mediaLibrary(editor)).containsExactly(cleanMedia);
    }

    @Test
    @DisplayName("TC-CMS-008: media library requires CONTENT_EDIT before reading")
    void mediaLibraryRejectsUsersWithoutContentEdit() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.mediaLibrary(employee))
                .isInstanceOf(ContentPermissionException.class);

        verify(store, never()).findAllMedia();
    }

    @Test
    @DisplayName("TC-CMS-013: unapproved conditions use a non-official fallback")
    void unapprovedRequestCopyReturnsLocalizedSafeFallback() {
        when(store.findTranslation("request-conditions", "es")).thenReturn(Optional.empty());

        var conditions = service.currentFor("es");

        assertThat(conditions.approved()).isFalse();
        assertThat(conditions.version()).isZero();
        assertThat(conditions.title()).contains("pendientes");
        assertThat(conditions.body().value()).contains("No hay condiciones oficiales aprobadas.");
    }

    @Test
    @DisplayName("TC-CMS-013: approved request copy is returned in its published locale and version")
    void approvedRequestCopyReturnsPublishedEnglishSnapshot() throws Exception {
        PageEntity page = new PageEntity(UUID.randomUUID(), "request-conditions",
                ContentPageType.REQUEST_CONDITIONS, Instant.now(clock));
        PageTranslationEntity translation = page.translation("en");
        translation.edit("request-conditions", "Request conditions", "Request conditions",
                "Approved request copy", ApprovalState.APPROVED, editor.userId(), Instant.now(clock));
        String snapshot = new ObjectMapper().writeValueAsString(new ContentSnapshot(
                "request-conditions", "Request conditions", "Request conditions", "Approved request copy", true,
                java.util.List.of(new ContentSnapshot.BlockSnapshot(
                        com.simbio.core.content.domain.ContentBlockType.TEXT, 0,
                        "Conditions", "Synthetic approved conditions", null, null))));
        translation.publish(snapshot, editor.userId(), Instant.now(clock));
        when(store.findTranslation("request-conditions", "en")).thenReturn(Optional.of(translation));

        var conditions = service.currentFor("en");

        assertThat(conditions.approved()).isTrue();
        assertThat(conditions.locale()).isEqualTo("en");
        assertThat(conditions.version()).isEqualTo(1);
        assertThat(conditions.title()).isEqualTo("Request conditions");
        assertThat(conditions.body().value()).contains("Synthetic approved conditions");
    }

    @Test
    @DisplayName("TC-CMS-004: locale fallback returns the source locale and canonical path")
    void publicResolutionFallsBackToPublishedTranslation() throws Exception {
        PageEntity page = publishedPage("about", "es", "sobre-nosotros", "Sobre nosotros");
        PageTranslationEntity translation = page.getTranslations().getFirst();
        when(store.findPublishedByLocaleAndSlug("en", "sobre-nosotros")).thenReturn(Optional.empty());
        when(store.findRedirect("en", "sobre-nosotros")).thenReturn(Optional.empty());
        when(store.findAnyPublishedBySlug("sobre-nosotros")).thenReturn(Optional.of(translation));

        var resolution = service.resolvePublic("en", "sobre-nosotros");

        assertThat(resolution.page().requestedLocale()).isEqualTo("en");
        assertThat(resolution.page().locale()).isEqualTo("es");
        assertThat(resolution.page().canonicalPath()).isEqualTo("/es/sobre-nosotros");
    }

    @Test
    @DisplayName("TC-CMS-004: renamed published slugs resolve as permanent redirects")
    void oldSlugReturnsPermanentRedirect() {
        when(store.findPublishedByLocaleAndSlug("es", "old-page")).thenReturn(Optional.empty());
        when(store.findRedirect("es", "old-page"))
                .thenReturn(Optional.of(new com.simbio.core.content.domain.SlugRedirectEntity(
                        "es", "old-page", "new-page", Instant.now(clock))));

        var resolution = service.resolvePublic("es", "old-page");

        assertThat(resolution.redirectPath()).isEqualTo("/es/new-page");
    }

    @Test
    @DisplayName("TC-CMS-002: publishing a renamed page evicts the stale public cache")
    void publicationInvalidatesCachedOldSlug() throws Exception {
        PageEntity page = publishedPage("about-us", "es", "old-page", "About");
        PageTranslationEntity translation = page.getTranslations().getFirst();
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findPublishedByLocaleAndSlug("es", "old-page"))
                .thenReturn(Optional.of(translation), Optional.empty());
        when(store.findPublishedByLocaleAndSlug("es", "new-page")).thenReturn(Optional.empty());
        when(store.findRedirect("es", "old-page"))
                .thenReturn(Optional.empty(), Optional.of(new com.simbio.core.content.domain.SlugRedirectEntity(
                        "es", "old-page", "new-page", Instant.now(clock))));
        when(store.saveTranslation(translation)).thenReturn(translation);
        when(store.saveVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.resolvePublic("es", "old-page");
        translation.edit("new-page", "About updated", "About", "About", ApprovalState.APPROVED,
                editor.userId(), Instant.now(clock));

        service.publish(editor, page.getPublicId(), "es");

        assertThat(service.resolvePublic("es", "old-page").redirectPath()).isEqualTo("/es/new-page");
    }

    @Test
    @DisplayName("TC-CMS-004: draft-only slugs are not publicly resolvable")
    void publicResolutionNeverReturnsDrafts() {
        when(store.findPublishedByLocaleAndSlug("es", "private-page")).thenReturn(Optional.empty());
        when(store.findRedirect("es", "private-page")).thenReturn(Optional.empty());
        when(store.findAnyPublishedBySlug("private-page")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolvePublic("es", "private-page"))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    @DisplayName("TC-CMS-005: authenticated preview renders the draft without publishing")
    void previewRendersCurrentDraftWithoutChangingPublicState() {
        PageEntity page = pageWithTranslation(ContentPageType.PAGE, ApprovalState.PENDING);
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));

        var preview = service.preview(editor, page.getPublicId(), "es");

        assertThat(preview.title()).isEqualTo("About");
        assertThat(preview.canonicalPath()).isEqualTo("/es/about");
        verify(store, never()).saveTranslation(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("TC-CMS-005: authenticated preview renders a hero media reference")
    void previewRendersHeroMedia() {
        UUID fileId = UUID.randomUUID();
        PageEntity page = pageWithTranslation(ContentPageType.PAGE, ApprovalState.PENDING);
        page.getTranslations().getFirst().replaceBlocks(java.util.List.of(
                new ContentBlockEntity(ContentBlockType.HERO, 0, "Hero", "", null, fileId)));
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findMedia(fileId)).thenReturn(Optional.of(new MediaAssetEntity(
                fileId, "Imagen", "Image", Instant.now(clock))));
        when(imageVariants.listVariants(eq(fileId), any()))
                .thenReturn(java.util.List.of(new ImageVariant("w640", 640, 360, "image/webp", 1000)));

        var preview = service.preview(editor, page.getPublicId(), "es");

        assertThat(preview.blocks().getFirst().imageUrl())
                .isEqualTo("/gestion/contenido/medios/" + fileId + "/preview");
        assertThat(preview.blocks().getFirst().imageSrcSet())
                .contains("/variants/w640 640w");
        assertThat(preview.blocks().getFirst().imageAlt()).isEqualTo("Imagen");
    }

    @Test
    @DisplayName("TC-CMS-006: allowed blocks keep their explicit order")
    void savesBlocksInPositionOrder() {
        UUID heroMediaId = UUID.randomUUID();
        when(store.findPageByKey("landing")).thenReturn(Optional.empty());
        when(store.savePage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.findMedia(heroMediaId)).thenReturn(Optional.of(new MediaAssetEntity(
                heroMediaId, "Imagen", "Image", Instant.now(clock))));
        SavePageCommand command = new SavePageCommand(null, "landing", ContentPageType.PAGE.name(), "es",
                "landing", "Landing", "Landing", "Description",
                java.util.List.of(
                        new BlockInput(ContentBlockType.TEXT.name(), 4,
                                "Second", "Second block", null, null),
                        new BlockInput(ContentBlockType.HERO.name(), 0,
                                "First", "First block", null, heroMediaId)));

        var draft = service.saveDraft(editor, command);

        assertThat(draft.blocks()).extracting(BlockInput::position).containsExactly(0, 4);
        assertThat(draft.blocks().getFirst().mediaPublicId()).isEqualTo(heroMediaId);
    }

    @Test
    @DisplayName("TC-CMS-006: unsafe block destinations are rejected before persistence")
    void rejectsDangerousBlockUrls() {
        SavePageCommand command = new SavePageCommand(null, "landing", ContentPageType.PAGE.name(), "es",
                "landing", "Landing", "Landing", "Description",
                java.util.List.of(new BlockInput(com.simbio.core.content.domain.ContentBlockType.CTA.name(), 0,
                        "Go", "", "javascript:alert(1)", null)));

        assertThatThrownBy(() -> service.saveDraft(editor, command))
                .isInstanceOf(ContentValidationException.class);

        verify(store, never()).savePage(any());
    }

    @Test
    @DisplayName("TC-CMS-006: unknown page and block type codes are rejected")
    void rejectsUnknownTypeCodes() {
        SavePageCommand invalidPageType = new SavePageCommand(null, "landing", "UNKNOWN", "es",
                "landing", "Landing", "Landing", "Description", java.util.List.of());
        SavePageCommand invalidBlockType = new SavePageCommand(null, "landing", ContentPageType.PAGE.name(), "es",
                "landing", "Landing", "Landing", "Description",
                java.util.List.of(new BlockInput("UNKNOWN", 0, "Heading", "Body", null, null)));

        assertThatThrownBy(() -> service.saveDraft(editor, invalidPageType))
                .isInstanceOf(ContentValidationException.class);
        assertThatThrownBy(() -> service.saveDraft(editor, invalidBlockType))
                .isInstanceOf(ContentValidationException.class);

        verify(store, never()).savePage(any());
    }

    @Test
    @DisplayName("TC-CMS-007: restore creates a new immutable version from the prior snapshot")
    void restoringPreviousSnapshotIncrementsVersionAndKeepsHistory() throws Exception {
        PageEntity page = pageWithTranslation(ContentPageType.PAGE, ApprovalState.APPROVED);
        PageTranslationEntity translation = page.getTranslations().getFirst();
        String oldSnapshot = new ObjectMapper().writeValueAsString(new ContentSnapshot(
                "old-page", "Old title", "Old SEO", "Old description", false,
                java.util.List.of(new ContentSnapshot.BlockSnapshot(
                        com.simbio.core.content.domain.ContentBlockType.TEXT, 0, "Previous", "Body", null, null))));
        translation.publish("{\"slug\":\"about\",\"title\":\"About\",\"seoTitle\":\"About\","
                + "\"metaDescription\":\"About\",\"approved\":true,\"blocks\":[]}", editor.userId(),
                Instant.now(clock));
        translation.edit("about", "Current", "Current", "Current description",
                ApprovalState.APPROVED, editor.userId(), Instant.now(clock));
        translation.publish("{\"slug\":\"about\",\"title\":\"Current\",\"seoTitle\":\"Current\","
                + "\"metaDescription\":\"Current description\",\"approved\":true,\"blocks\":[]}", editor.userId(),
                Instant.now(clock));
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findVersions(translation.getPublicId())).thenReturn(java.util.List.of(
                new com.simbio.core.content.domain.PageVersionEntity(translation.getPublicId(), 1,
                        oldSnapshot, editor.userId(), Instant.now(clock))));
        when(store.saveTranslation(translation)).thenReturn(translation);
        when(store.saveVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.restorePrevious(editor, page.getPublicId(), "es");

        assertThat(translation.getVersion()).isEqualTo(3);
        assertThat(translation.getPublishedSlug()).isEqualTo("old-page");
        assertThat(translation.getPublishedSnapshotJson()).contains("\"title\":\"Old title\"");
        verify(store).saveVersion(org.mockito.ArgumentMatchers.argThat(version ->
                version.getVersion() == 3 && version.getSnapshotJson().equals(oldSnapshot)));
        verify(eventPublisher).publishEvent(any(PagePublished.class));
    }

    @Test
    @DisplayName("TC-CMS-008: publication rejects referenced media that is no longer clean")
    void publicationRejectsMediaWithoutCleanFileMetadata() {
        UUID fileId = UUID.randomUUID();
        PageEntity page = new PageEntity(UUID.randomUUID(), "image-page", ContentPageType.PAGE, Instant.now(clock));
        PageTranslationEntity translation = page.translation("es");
        translation.edit("image-page", "Image page", "Image page", "Description",
                ApprovalState.PENDING, editor.userId(), Instant.now(clock));
        translation.replaceBlocks(java.util.List.of(new com.simbio.core.content.domain.ContentBlockEntity(
                com.simbio.core.content.domain.ContentBlockType.IMAGE, 0, "Image", "", null, fileId)));
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findMedia(fileId)).thenReturn(Optional.of(new MediaAssetEntity(
                fileId, "Imagen", "Image", Instant.now(clock))));
        when(cleanFileMetadataReader.findClean(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(editor, page.getPublicId(), "es"))
                .isInstanceOf(ContentValidationException.class);

        verify(store, never()).saveTranslation(any());
    }

    @Test
    @DisplayName("TC-CMS-008: publication rejects hero media that is no longer clean")
    void publicationRejectsUncleanHeroMedia() {
        UUID fileId = UUID.randomUUID();
        PageEntity page = pageWithTranslation(ContentPageType.PAGE, ApprovalState.PENDING);
        PageTranslationEntity translation = page.getTranslations().getFirst();
        translation.replaceBlocks(java.util.List.of(new ContentBlockEntity(
                ContentBlockType.HERO, 0, "Hero", "", null, fileId)));
        when(store.findPageByPublicId(page.getPublicId())).thenReturn(Optional.of(page));
        when(store.findMedia(fileId)).thenReturn(Optional.of(new MediaAssetEntity(
                fileId, "Imagen", "Image", Instant.now(clock))));
        when(cleanFileMetadataReader.findClean(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(editor, page.getPublicId(), "es"))
                .isInstanceOf(ContentValidationException.class);

        verify(store, never()).saveTranslation(any());
    }

    private PageEntity publishedPage(String pageKey, String locale, String slug, String title) throws Exception {
        PageEntity page = new PageEntity(UUID.randomUUID(), pageKey, ContentPageType.PAGE, Instant.now(clock));
        PageTranslationEntity translation = page.translation(locale);
        translation.edit(slug, title, title, title, ApprovalState.APPROVED, editor.userId(), Instant.now(clock));
        String snapshot = new ObjectMapper().writeValueAsString(
                new ContentSnapshot(slug, title, title, title, true, java.util.List.of()));
        translation.publish(snapshot, editor.userId(), Instant.now(clock));
        return page;
    }

    private PageEntity pageWithTranslation(ContentPageType type, ApprovalState approvalState) {
        PageEntity page = new PageEntity(UUID.randomUUID(), "test-page", type, Instant.now(clock));
        PageTranslationEntity translation = page.translation("es");
        translation.edit("about", "About", "About", "Safe page", approvalState, editor.userId(), Instant.now(clock));
        return page;
    }
}
