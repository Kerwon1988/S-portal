package com.simbio.core.content.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.api.PagePublished;
import com.simbio.core.content.api.RequestConditions;
import com.simbio.core.content.api.RequestConditionsReader;
import com.simbio.core.content.api.SanitizedHtml;
import com.simbio.core.content.domain.ApprovalState;
import com.simbio.core.content.domain.ContentBlockEntity;
import com.simbio.core.content.domain.ContentBlockType;
import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.MediaAssetEntity;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.content.domain.PageVersionEntity;
import com.simbio.core.content.domain.SlugRedirectEntity;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.CleanFileMetadataReader;
import com.simbio.core.files.api.ImageVariant;
import com.simbio.core.files.api.ImageVariants;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService implements RequestConditionsReader {

    private static final Pattern PAGE_KEY = Pattern.compile("[a-z][a-z0-9-]{0,119}");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*(?:/[a-z0-9]+(?:-[a-z0-9]+)*)*");
    private static final UUID SYSTEM_ACTOR = new UUID(0, 0);

    private final ContentStore store;
    private final ContentSanitizer sanitizer;
    private final FileStorage fileStorage;
    private final CleanFileMetadataReader cleanFileMetadataReader;
    private final ImageVariants imageVariants;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, PublishedPageView> publishedCache = new ConcurrentHashMap<>();

    @Autowired
    public ContentService(ContentStore store, ContentSanitizer sanitizer, FileStorage fileStorage,
            CleanFileMetadataReader cleanFileMetadataReader, ImageVariants imageVariants,
            AuditRecorder auditRecorder, ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this(store, sanitizer, fileStorage, cleanFileMetadataReader, imageVariants, auditRecorder,
                eventPublisher, objectMapper,
                Clock.systemUTC());
    }

    @Transactional(readOnly = true)
    public String homeSlug(String locale) {
        String safeLocale = normalizeLocale(locale);
        return store.findTranslation("home", safeLocale)
                .filter(value -> value.getPublishedSnapshotJson() != null)
                .map(PageTranslationEntity::getPublishedSlug)
                .orElseGet(() -> store.findTranslation("home", safeLocale.equals("es") ? "en" : "es")
                        .filter(value -> value.getPublishedSnapshotJson() != null)
                        .map(PageTranslationEntity::getPublishedSlug)
                        .orElseThrow(ContentNotFoundException::new));
    }

    ContentService(ContentStore store, ContentSanitizer sanitizer, FileStorage fileStorage,
            CleanFileMetadataReader cleanFileMetadataReader, ImageVariants imageVariants,
            AuditRecorder auditRecorder, ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper,
            Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.sanitizer = Objects.requireNonNull(sanitizer);
        this.fileStorage = Objects.requireNonNull(fileStorage);
        this.cleanFileMetadataReader = Objects.requireNonNull(cleanFileMetadataReader);
        this.imageVariants = Objects.requireNonNull(imageVariants);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public PageDraftView saveDraft(AuthenticatedUser actor, SavePageCommand command) {
        requireEditor(actor);
        ContentPageType pageType = validateCommand(command);
        PageEntity page;
        if (command.pagePublicId() == null) {
            page = store.findPageByKey(command.pageKey())
                    .orElseGet(() -> new PageEntity(UUID.randomUUID(), command.pageKey(),
                            pageType, Instant.now(clock)));
        } else {
            page = store.findPageByPublicId(command.pagePublicId()).orElseThrow(ContentNotFoundException::new);
            if (!page.getPageKey().equals(command.pageKey()) || page.getPageType() != pageType) {
                throw new ContentValidationException("Page identity and type cannot be changed");
            }
        }
        if (page.getPageType() != pageType) {
            throw new ContentValidationException("The page key already belongs to a different content type");
        }
        PageTranslationEntity translation = page.translation(command.locale());
        ApprovalState approval = page.getPageType() == ContentPageType.LEGAL
                || page.getPageType() == ContentPageType.REQUEST_CONDITIONS
                        ? ApprovalState.PENDING : translation.getApprovalState();
        translation.edit(command.slug(), required(command.title(), 200, "title"),
                bounded(command.seoTitle(), 200, "SEO title"),
                bounded(command.metaDescription(), 320, "meta description"),
                approval, actor.userId(), Instant.now(clock));
        translation.replaceBlocks(command.blocks().stream()
                .sorted(Comparator.comparingInt(BlockInput::position))
                .map(this::toEntity)
                .toList());
        store.savePage(page);
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_DRAFT_SAVED", "CONTENT_PAGE",
                page.getPublicId(), null));
        return draftView(page, translation);
    }

    @Transactional(readOnly = true)
    public List<PageDraftView> listForEditor(AuthenticatedUser actor) {
        requireEditor(actor);
        return store.findAllTranslations().stream()
                .map(translation -> draftView(translation.getPage(), translation))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageDraftView draft(AuthenticatedUser actor, UUID pagePublicId, String locale) {
        requireEditor(actor);
        PageEntity page = store.findPageByPublicId(pagePublicId).orElseThrow(ContentNotFoundException::new);
        return page.getTranslations().stream()
                .filter(translation -> translation.getLocale().equals(normalizeLocale(locale)))
                .findFirst()
                .map(translation -> draftView(page, translation))
                .orElseThrow(ContentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public PublishedPageView preview(AuthenticatedUser actor, UUID pagePublicId, String locale) {
        requireEditor(actor);
        PageDraftView draft = draft(actor, pagePublicId, locale);
        PageEntity page = store.findPageByPublicId(pagePublicId).orElseThrow(ContentNotFoundException::new);
        PageTranslationEntity translation = page.getTranslations().stream()
                .filter(value -> value.getLocale().equals(normalizeLocale(locale)))
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        return render(page, translation, draft);
    }

    @Transactional
    public PagePublished publish(AuthenticatedUser actor, UUID pagePublicId, String locale) {
        requireEditor(actor);
        PageEntity page = store.findPageByPublicId(pagePublicId).orElseThrow(ContentNotFoundException::new);
        PageTranslationEntity translation = page.getTranslations().stream()
                .filter(value -> value.getLocale().equals(normalizeLocale(locale)))
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        if ((page.getPageType() == ContentPageType.LEGAL
                || page.getPageType() == ContentPageType.REQUEST_CONDITIONS)
                && translation.getApprovalState() != ApprovalState.APPROVED) {
            throw new ContentApprovalRequiredException();
        }
        validatePublication(translation);
        store.findPublishedByLocaleAndSlug(translation.getLocale(), translation.getSlug())
                .filter(existing -> !existing.getPublicId().equals(translation.getPublicId()))
                .ifPresent(existing -> {
                    throw new ContentValidationException("Slug is already published for this locale");
                });
        if (!translation.getSlug().equals(translation.getPublishedSlug())) {
            if (translation.getPublishedSlug() != null) {
                store.findRedirect(translation.getLocale(), translation.getPublishedSlug())
                        .ifPresentOrElse(redirect -> {
                            if (!redirect.getNewSlug().equals(translation.getSlug())) {
                                throw new ContentValidationException("The old slug already redirects elsewhere");
                            }
                        }, () -> store.saveRedirect(new SlugRedirectEntity(translation.getLocale(),
                                translation.getPublishedSlug(), translation.getSlug(), Instant.now(clock))));
            }
        }
        int nextVersion = translation.getVersion() + 1;
        ContentSnapshot snapshot = ContentSnapshot.from(draftView(page, translation),
                translation.getApprovalState() == ApprovalState.APPROVED);
        String snapshotJson = serialize(snapshot);
        Instant publishedAt = Instant.now(clock);
        translation.publish(snapshotJson, actor.userId(), publishedAt);
        store.saveTranslation(translation);
        store.saveVersion(new PageVersionEntity(translation.getPublicId(), nextVersion, snapshotJson,
                actor.userId(), publishedAt));
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_PAGE_PUBLISHED", "CONTENT_PAGE",
                page.getPublicId(), null));
        publishedCache.clear();
        PagePublished published = new PagePublished(page.getPublicId(), translation.getPublicId());
        eventPublisher.publishEvent(published);
        return published;
    }

    @Transactional
    public PagePublished restorePrevious(AuthenticatedUser actor, UUID pagePublicId, String locale) {
        requireEditor(actor);
        PageEntity page = store.findPageByPublicId(pagePublicId).orElseThrow(ContentNotFoundException::new);
        PageTranslationEntity translation = page.getTranslations().stream()
                .filter(value -> value.getLocale().equals(normalizeLocale(locale)))
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        PageVersionEntity previous = store.findVersions(translation.getPublicId()).stream()
                .filter(version -> version.getVersion() < translation.getVersion())
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        ContentSnapshot snapshot = deserialize(previous.getSnapshotJson());
        List<ContentBlockEntity> restoredBlocks = snapshot.blocks().stream()
                .map(block -> new ContentBlockEntity(block.type(), block.position(), block.heading(),
                        block.bodyMarkdown(), block.targetUrl(), block.mediaPublicId()))
                .toList();
        translation.restoreDraft(snapshot.slug(), snapshot.title(), snapshot.seoTitle(), snapshot.metaDescription(),
                restoredBlocks, actor.userId(), Instant.now(clock));
        validatePublication(translation);
        int nextVersion = translation.getVersion() + 1;
        String snapshotJson = serialize(snapshot);
        Instant restoredAt = Instant.now(clock);
        translation.publish(snapshotJson, actor.userId(), restoredAt);
        store.saveTranslation(translation);
        store.saveVersion(new PageVersionEntity(translation.getPublicId(), nextVersion, snapshotJson,
                actor.userId(), restoredAt));
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_PAGE_RESTORED", "CONTENT_PAGE",
                page.getPublicId(), null));
        publishedCache.clear();
        PagePublished published = new PagePublished(page.getPublicId(), translation.getPublicId());
        eventPublisher.publishEvent(published);
        return published;
    }

    @Transactional(readOnly = true)
    public List<PageVersionEntity> versions(AuthenticatedUser actor, UUID translationPublicId) {
        requireEditor(actor);
        if (store.findTranslationByPublicId(translationPublicId).isEmpty()) {
            throw new ContentNotFoundException();
        }
        return store.findVersions(translationPublicId);
    }

    @Transactional(readOnly = true)
    public PageResolution resolvePublic(String locale, String slug) {
        String safeLocale = normalizeLocale(locale);
        String safeSlug = normalizeSlug(slug);
        String key = safeLocale + ":" + safeSlug;
        PublishedPageView cached = publishedCache.get(key);
        if (cached != null) {
            return PageResolution.found(cached);
        }
        return store.findPublishedByLocaleAndSlug(safeLocale, safeSlug)
                .map(translation -> {
                    PublishedPageView view = publishedView(translation, safeLocale);
                    publishedCache.put(key, view);
                    return PageResolution.found(view);
                })
                .orElseGet(() -> store.findRedirect(safeLocale, safeSlug)
                        .map(redirect -> PageResolution.redirected("/" + safeLocale + "/" + redirect.getNewSlug()))
                        .orElseGet(() -> store.findAnyPublishedBySlug(safeSlug)
                                .map(translation -> PageResolution.found(
                                        publishedView(translation, safeLocale)))
                                .orElseThrow(ContentNotFoundException::new)));
    }

    @Transactional(readOnly = true)
    public PublishedPageView home(String locale) {
        String safeLocale = normalizeLocale(locale);
        PageTranslationEntity translation = store.findTranslation("home", safeLocale)
                .filter(value -> value.getPublishedSnapshotJson() != null)
                .orElseGet(() -> store.findTranslation("home", safeLocale.equals("es") ? "en" : "es")
                        .filter(value -> value.getPublishedSnapshotJson() != null)
                        .orElseThrow(ContentNotFoundException::new));
        return publishedView(translation, safeLocale);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestConditions currentFor(String locale) {
        String safeLocale = normalizeLocale(locale);
        return store.findTranslation("request-conditions", safeLocale)
                .filter(value -> value.getPublishedSnapshotJson() != null)
                .map(translation -> Map.entry(translation,
                        deserialize(translation.getPublishedSnapshotJson())))
                .filter(entry -> entry.getValue().approved())
                .map(entry -> {
                    PageTranslationEntity translation = entry.getKey();
                    ContentSnapshot snapshot = entry.getValue();
                    String body = snapshot.blocks().stream().map(ContentSnapshot.BlockSnapshot::bodyMarkdown)
                            .filter(value -> !value.isBlank()).findFirst().orElse("");
                    return new RequestConditions(safeLocale, true, translation.getVersion(), snapshot.title(),
                            sanitizer.sanitize(body));
                })
                .orElseGet(() -> new RequestConditions(safeLocale, false, 0,
                        safeLocale.equals("es") ? "Condiciones pendientes de aprobación"
                                : "Conditions pending approval",
                        sanitizer.sanitize(safeLocale.equals("es")
                                ? "No hay condiciones oficiales aprobadas."
                                : "There are no approved official conditions.")));
    }

    @Transactional
    public MediaAssetEntity storeMedia(AuthenticatedUser actor, InputStream input, String submittedName,
            long declaredSize, String altEs, String altEn) {
        requireEditor(actor);
        String safeAltEs = required(altEs, 500, "Spanish alt text");
        String safeAltEn = required(altEn, 500, "English alt text");
        StoredFile stored = fileStorage.store(input, submittedName, declaredSize, FilePurpose.IMAGE);
        if (stored.scanState() != ScanState.CLEAN) {
            throw new ContentMediaNotCleanException();
        }
        imageVariants.createVariants(stored.publicId(), requested -> requested.equals(stored.publicId()));
        MediaAssetEntity asset = store.saveMedia(new MediaAssetEntity(stored.publicId(), safeAltEs, safeAltEn,
                Instant.now(clock)));
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_MEDIA_UPLOADED", "CONTENT_MEDIA",
                stored.publicId(), null));
        return asset;
    }

    @Transactional(readOnly = true)
    public List<MediaAssetEntity> mediaLibrary(AuthenticatedUser actor) {
        requireEditor(actor);
        return store.findAllMedia().stream()
                .filter(media -> cleanFileMetadataReader.findClean(media.getFilePublicId()).isPresent())
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicMedia openPublicMedia(UUID publicId) {
        MediaAssetEntity media = store.findMedia(publicId).orElseThrow(ContentNotFoundException::new);
        if (!store.isPublishedMedia(publicId)) {
            throw new ContentNotFoundException();
        }
        String detectedMime = cleanFileMetadataReader.findClean(publicId)
                .map(com.simbio.core.files.api.CleanFileMetadata::detectedMime)
                .orElseThrow(ContentNotFoundException::new);
        InputStream input = fileStorage.open(publicId, requested -> requested.equals(publicId)
                && store.isPublishedMedia(requested));
        return new PublicMedia(publicId, detectedMime, input);
    }

    @Transactional(readOnly = true)
    public PublicMedia openPublicImageVariant(UUID publicId, String variantCode) {
        if (publicId == null || !store.isPublishedMedia(publicId)) {
            throw new ContentNotFoundException();
        }
        com.simbio.core.files.api.FileAccessAuthorization authorization =
                requested -> requested.equals(publicId) && store.isPublishedMedia(requested);
        ImageVariant variant = imageVariants.listVariants(publicId, authorization).stream()
                .filter(candidate -> candidate.code().equals(variantCode))
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        return new PublicMedia(publicId, variant.mime(),
                imageVariants.openVariant(publicId, variantCode, authorization));
    }

    @Transactional(readOnly = true)
    public PublicMedia openPreviewMedia(AuthenticatedUser actor, UUID publicId) {
        requireEditor(actor);
        MediaAssetEntity media = store.findMedia(publicId).orElseThrow(ContentNotFoundException::new);
        String detectedMime = cleanFileMetadataReader.findClean(publicId)
                .map(com.simbio.core.files.api.CleanFileMetadata::detectedMime)
                .orElseThrow(ContentNotFoundException::new);
        InputStream input = fileStorage.open(publicId, requested -> requested.equals(publicId));
        return new PublicMedia(publicId, detectedMime, input);
    }

    @Transactional(readOnly = true)
    public PublicMedia openPreviewImageVariant(AuthenticatedUser actor, UUID publicId, String variantCode) {
        requireEditor(actor);
        if (store.findMedia(publicId).isEmpty()) {
            throw new ContentNotFoundException();
        }
        com.simbio.core.files.api.FileAccessAuthorization authorization = requested -> requested.equals(publicId);
        ImageVariant variant = imageVariants.listVariants(publicId, authorization).stream()
                .filter(candidate -> candidate.code().equals(variantCode))
                .findFirst()
                .orElseThrow(ContentNotFoundException::new);
        return new PublicMedia(publicId, variant.mime(),
                imageVariants.openVariant(publicId, variantCode, authorization));
    }

    @Transactional(readOnly = true)
    public List<PublishedPageView> sitemapPages() {
        return store.findPublishedTranslations().stream()
                .filter(translation -> translation.getPage().getPageType() != ContentPageType.REQUEST_CONDITIONS)
                .map(translation -> publishedView(translation, translation.getLocale()))
                .toList();
    }

    private PublishedPageView publishedView(PageTranslationEntity translation, String requestedLocale) {
        PageEntity page = translation.getPage();
        ContentSnapshot snapshot = deserialize(translation.getPublishedSnapshotJson());
        Map<String, String> alternatePaths = page.getTranslations().stream()
                .filter(alternate -> alternate.getPublishedSnapshotJson() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        PageTranslationEntity::getLocale,
                        alternate -> page.getPageKey().equals("home")
                                ? "/" + alternate.getLocale()
                                : "/" + alternate.getLocale() + "/" + alternate.getPublishedSlug()));
        List<ContentBlockView> blockViews = snapshot.blocks().stream()
                .sorted(Comparator.comparingInt(ContentSnapshot.BlockSnapshot::position))
                .map(block -> renderBlock(block, translation.getLocale(), false))
                .toList();
        String canonicalPath = page.getPageKey().equals("home")
                ? "/" + translation.getLocale()
                : "/" + translation.getLocale() + "/" + snapshot.slug();
        return new PublishedPageView(page.getPublicId(), page.getPageKey(), page.getPageType(), requestedLocale,
                translation.getLocale(), snapshot.slug(), snapshot.title(), snapshot.seoTitle(),
                snapshot.metaDescription(), canonicalPath,
                alternatePaths, translation.getVersion(), blockViews);
    }

    private PublishedPageView render(PageEntity page, PageTranslationEntity translation, PageDraftView draft) {
        List<ContentBlockView> blockViews = draft.blocks().stream()
                .sorted(Comparator.comparingInt(BlockInput::position))
                .map(block -> renderBlock(new ContentSnapshot.BlockSnapshot(parseBlockType(block.type()),
                        block.position(),
                        block.heading(), block.bodyMarkdown(), block.targetUrl(), block.mediaPublicId()),
                        translation.getLocale(), true))
                .toList();
        return new PublishedPageView(page.getPublicId(), page.getPageKey(), page.getPageType(),
                translation.getLocale(), translation.getLocale(), draft.slug(), draft.title(), draft.seoTitle(),
                draft.metaDescription(), "/" + translation.getLocale() + "/" + draft.slug(), Map.of(),
                translation.getVersion(), blockViews);
    }

    private ContentBlockView renderBlock(ContentSnapshot.BlockSnapshot block, String locale, boolean preview) {
        SanitizedHtml body = sanitizer.sanitize(block.bodyMarkdown());
        if (block.mediaPublicId() == null
                || (block.type() != ContentBlockType.IMAGE && block.type() != ContentBlockType.HERO)) {
            return new ContentBlockView(block.type(), block.position(), block.heading(), body, block.targetUrl(),
                    null, null, null, "");
        }
        MediaAssetEntity asset = store.findMedia(block.mediaPublicId()).orElseThrow(ContentNotFoundException::new);
        com.simbio.core.files.api.FileAccessAuthorization authorization = preview
                ? requested -> requested.equals(block.mediaPublicId())
                : requested -> requested.equals(block.mediaPublicId()) && store.isPublishedMedia(requested);
        String mediaPrefix = preview
                ? "/gestion/contenido/medios/" + block.mediaPublicId() + "/preview"
                : "/public/media/" + block.mediaPublicId();
        String srcSet = imageVariants.listVariants(block.mediaPublicId(), authorization).stream()
                .map(variant -> mediaPrefix + "/variants/" + variant.code() + " " + variant.width() + "w")
                .collect(java.util.stream.Collectors.joining(", "));
        return new ContentBlockView(block.type(), block.position(), block.heading(), body, block.targetUrl(),
                block.mediaPublicId(), mediaPrefix, srcSet, asset.altFor(locale));
    }

    private PageDraftView draftView(PageEntity page, PageTranslationEntity translation) {
        return new PageDraftView(page.getPublicId(), translation.getPublicId(), page.getPageKey(),
                page.getPageType(), translation.getLocale(), translation.getSlug(), translation.getTitle(),
                translation.getSeoTitle(), translation.getMetaDescription(), translation.getState(),
                translation.getVersion(), translation.getBlocks().stream()
                        .map(block -> new BlockInput(block.getType().name(), block.getPosition(), block.getHeading(),
                                block.getBodyMarkdown(), block.getTargetUrl(), block.getMediaPublicId()))
                        .toList());
    }

    private ContentBlockEntity toEntity(BlockInput block) {
        return new ContentBlockEntity(parseBlockType(block.type()), block.position(), block.heading(),
                block.bodyMarkdown(), block.targetUrl(), block.mediaPublicId());
    }

    private ContentPageType validateCommand(SavePageCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.pageKey() == null || !PAGE_KEY.matcher(command.pageKey()).matches()) {
            throw new ContentValidationException("Page key is invalid");
        }
        ContentPageType pageType = parsePageType(command.pageType());
        normalizeLocale(command.locale());
        normalizeSlug(command.slug());
        required(command.title(), 200, "title");
        bounded(command.seoTitle(), 200, "SEO title");
        bounded(command.metaDescription(), 320, "meta description");
        if (command.blocks().size() > 100) {
            throw new ContentValidationException("A page cannot contain more than 100 blocks");
        }
        int contentLength = command.title().length() + bounded(command.seoTitle(), 200, "SEO title").length()
                + bounded(command.metaDescription(), 320, "meta description").length()
                + command.blocks().stream().mapToInt(block -> block == null || block.bodyMarkdown() == null
                        ? 0 : block.bodyMarkdown().length()).sum();
        if (contentLength > 12000) {
            throw new ContentValidationException("Page content exceeds the maximum snapshot size");
        }
        java.util.HashSet<Integer> positions = new java.util.HashSet<>();
        for (BlockInput block : command.blocks()) {
            validateBlock(block, positions);
        }
        return pageType;
    }

    private void validateBlock(BlockInput block, java.util.Set<Integer> positions) {
        if (block == null || block.type() == null || block.type().isBlank()
                || block.position() < 0 || !positions.add(block.position())) {
            throw new ContentValidationException("Block type and unique non-negative position are required");
        }
        ContentBlockType type = parseBlockType(block.type());
        required(block.heading(), 200, "block heading");
        if (block.bodyMarkdown() == null || block.bodyMarkdown().length() > 8000) {
            throw new ContentValidationException("Block text exceeds the maximum length");
        }
        sanitizer.sanitize(block.bodyMarkdown());
        if (block.targetUrl() != null && !block.targetUrl().isBlank()) {
            validateUrl(block.targetUrl());
        }
        if (type == ContentBlockType.IMAGE) {
            if (block.mediaPublicId() == null || store.findMedia(block.mediaPublicId()).isEmpty()) {
                throw new ContentValidationException("Image block must reference a registered media asset");
            }
        } else if (type == ContentBlockType.HERO && block.mediaPublicId() != null) {
            if (store.findMedia(block.mediaPublicId()).isEmpty()) {
                throw new ContentValidationException("Hero media must reference a registered media asset");
            }
        } else if (block.mediaPublicId() != null) {
            throw new ContentValidationException("Only hero and image blocks may reference media");
        }
        if (type == ContentBlockType.CTA && (block.targetUrl() == null || block.targetUrl().isBlank())) {
            throw new ContentValidationException("CTA blocks require a safe target URL");
        }
    }

    private static ContentPageType parsePageType(String value) {
        if (value == null || value.isBlank()) {
            throw new ContentValidationException("Page type is required");
        }
        try {
            return ContentPageType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ContentValidationException("Invalid content page type");
        }
    }

    private static ContentBlockType parseBlockType(String value) {
        try {
            return ContentBlockType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ContentValidationException("Invalid content block type");
        }
    }

    private void validatePublication(PageTranslationEntity translation) {
        if (translation.getTitle().isBlank() || translation.getSlug().isBlank()) {
            throw new ContentValidationException("A title and slug are required to publish");
        }
        for (ContentBlockEntity block : translation.getBlocks()) {
            boolean imageBlock = block.getType() == ContentBlockType.IMAGE;
            boolean heroWithMedia = block.getType() == ContentBlockType.HERO && block.getMediaPublicId() != null;
            if (imageBlock && block.getMediaPublicId() == null) {
                throw new ContentValidationException("Image block must reference a registered media asset");
            }
            if (block.getMediaPublicId() != null && !imageBlock && !heroWithMedia) {
                throw new ContentValidationException("Only hero and image blocks may reference media");
            }
            if (imageBlock || heroWithMedia) {
                MediaAssetEntity media = store.findMedia(block.getMediaPublicId())
                        .orElseThrow(() -> new ContentValidationException("Referenced media is not registered"));
                cleanFileMetadataReader.findClean(block.getMediaPublicId())
                        .orElseThrow(() -> new ContentValidationException("Referenced media is not clean"));
                if (media.getAltEs().isBlank() || media.getAltEn().isBlank()) {
                    throw new ContentValidationException("Media alt text is required in both locales");
                }
            }
        }
    }

    private static void validateUrl(String value) {
        if (value.startsWith("/") && !value.startsWith("//") && !value.contains("\\")
                && value.chars().noneMatch(Character::isISOControl)) {
            return;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("mailto"))) {
                throw new ContentValidationException("Only local, HTTPS, and mailto links are allowed");
            }
        } catch (IllegalArgumentException exception) {
            throw new ContentValidationException("Target URL is invalid");
        }
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || !(locale.equals("es") || locale.equals("en"))) {
            throw new ContentValidationException("Only es and en locales are supported");
        }
        return locale;
    }

    private static String normalizeSlug(String slug) {
        if (slug == null || slug.length() > 120 || !SLUG.matcher(slug).matches()
                || slug.contains("..") || slug.startsWith("/") || slug.endsWith("/")) {
            throw new ContentValidationException("Slug is invalid");
        }
        return slug;
    }

    private static String required(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ContentValidationException("Invalid " + field);
        }
        return value.trim();
    }

    private static String bounded(String value, int maxLength, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new ContentValidationException("Invalid " + field);
        }
        return normalized;
    }

    private static void requireEditor(AuthenticatedUser actor) {
        if (actor == null || !actor.hasPermission(Permission.CONTENT_EDIT)) {
            throw new ContentPermissionException();
        }
    }

    private String serialize(ContentSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize content snapshot", exception);
        }
    }

    private ContentSnapshot deserialize(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, ContentSnapshot.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not read content snapshot", exception);
        }
    }
}
