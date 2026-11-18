package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.content.application.ContentStore;
import com.simbio.core.content.domain.MediaAssetEntity;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.content.domain.PageVersionEntity;
import com.simbio.core.content.domain.SlugRedirectEntity;
import com.simbio.core.content.domain.TranslationState;

@Repository
@Transactional(readOnly = true)
class JpaContentStore implements ContentStore {

    private final PageJpaRepository pages;
    private final PageTranslationJpaRepository translations;
    private final PageVersionJpaRepository versions;
    private final SlugRedirectJpaRepository redirects;
    private final MediaAssetJpaRepository media;
    private final ContentBlockJpaRepository blocks;
    private final LegalDocumentJpaRepository legalDocuments;
    private final TeamMemberJpaRepository teamMembers;
    private final TeamMemberTranslationJpaRepository teamMemberTranslations;

    JpaContentStore(PageJpaRepository pages, PageTranslationJpaRepository translations,
            PageVersionJpaRepository versions, SlugRedirectJpaRepository redirects,
            MediaAssetJpaRepository media, ContentBlockJpaRepository blocks,
            LegalDocumentJpaRepository legalDocuments, TeamMemberJpaRepository teamMembers,
            TeamMemberTranslationJpaRepository teamMemberTranslations) {
        this.pages = pages;
        this.translations = translations;
        this.versions = versions;
        this.redirects = redirects;
        this.media = media;
        this.blocks = blocks;
        this.legalDocuments = legalDocuments;
        this.teamMembers = teamMembers;
        this.teamMemberTranslations = teamMemberTranslations;
    }

    @Override
    public Optional<PageEntity> findPageByPublicId(UUID publicId) {
        return pages.findByPublicId(publicId);
    }

    @Override
    public Optional<PageEntity> findPageByKey(String pageKey) {
        return pages.findByPageKey(pageKey);
    }

    @Override
    public Optional<PageTranslationEntity> findTranslation(String pageKey, String locale) {
        return translations.findByPage_PageKeyAndLocale(pageKey, locale);
    }

    @Override
    public Optional<PageTranslationEntity> findTranslationByPublicId(UUID publicId) {
        return translations.findByPublicId(publicId);
    }

    @Override
    public Optional<PageTranslationEntity> findByLocaleAndSlug(String locale, String slug) {
        return translations.findByLocaleAndSlug(locale, slug);
    }

    @Override
    public Optional<PageTranslationEntity> findPublishedByLocaleAndSlug(String locale, String slug) {
        return translations.findByLocaleAndPublishedSlug(locale, slug)
                .filter(translation -> translation.getPublishedSnapshotJson() != null);
    }

    @Override
    public Optional<PageTranslationEntity> findAnyPublishedBySlug(String slug) {
        return translations.findFirstByPublishedSlugAndPublishedSnapshotJsonIsNotNull(slug);
    }

    @Override
    public List<PageTranslationEntity> findAllTranslations() {
        return translations.findAllByOrderByUpdatedAtDesc();
    }

    @Override
    public List<PageTranslationEntity> findPublishedTranslations() {
        return translations.findAllByPublishedSnapshotJsonIsNotNullOrderByLocaleAscPublishedSlugAsc();
    }

    @Override
    @Transactional
    public PageEntity savePage(PageEntity page) {
        return pages.save(page);
    }

    @Override
    @Transactional
    public PageTranslationEntity saveTranslation(PageTranslationEntity translation) {
        return translations.save(translation);
    }

    @Override
    @Transactional
    public PageVersionEntity saveVersion(PageVersionEntity version) {
        return versions.save(version);
    }

    @Override
    public List<PageVersionEntity> findVersions(UUID translationPublicId) {
        return versions.findByTranslationPublicIdOrderByVersionDesc(translationPublicId);
    }

    @Override
    public Optional<PageVersionEntity> findVersion(UUID translationPublicId, int version) {
        return versions.findByTranslationPublicIdAndVersion(translationPublicId, version);
    }

    @Override
    public Optional<SlugRedirectEntity> findRedirect(String locale, String oldSlug) {
        return redirects.findByLocaleAndOldSlug(locale, oldSlug);
    }

    @Override
    @Transactional
    public SlugRedirectEntity saveRedirect(SlugRedirectEntity redirect) {
        return redirects.save(redirect);
    }

    @Override
    public Optional<MediaAssetEntity> findMedia(UUID filePublicId) {
        return media.findByFilePublicId(filePublicId);
    }

    @Override
    public List<MediaAssetEntity> findAllMedia() {
        return media.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public MediaAssetEntity saveMedia(MediaAssetEntity asset) {
        return media.save(asset);
    }

    @Override
    public boolean isPublishedMedia(UUID filePublicId) {
        return blocks.existsInPublishedContent(filePublicId)
                || teamMemberTranslations.existsInActiveMember(filePublicId);
    }

    @Override
    @Transactional
    public com.simbio.core.content.domain.LegalDocumentVersionEntity saveLegalDocument(
            com.simbio.core.content.domain.LegalDocumentVersionEntity document) {
        return legalDocuments.save(document);
    }

    @Override
    public List<com.simbio.core.content.domain.LegalDocumentVersionEntity> findLegalDocuments() {
        return legalDocuments.findAllByOrderByDocumentTypeAscLocaleAscVersionDesc();
    }

    @Override
    public Optional<com.simbio.core.content.domain.LegalDocumentVersionEntity> findLatestLegalDocument(
            String documentType, String locale) {
        return legalDocuments.findFirstByDocumentTypeAndLocaleOrderByVersionDesc(documentType, locale);
    }

    @Override
    public Optional<com.simbio.core.content.domain.LegalDocumentVersionEntity> findCurrentLegalDocument(
            String documentType, String locale, java.time.Instant effectiveAt) {
        return legalDocuments.findCurrent(documentType, locale, "APPROVED", effectiveAt,
                org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst();
    }

    @Override
    public List<com.simbio.core.content.domain.TeamMemberEntity> findTeamMembers() {
        return teamMembers.findAllByOrderByPositionAsc();
    }

    @Override
    public Optional<com.simbio.core.content.domain.TeamMemberEntity> findTeamMember(UUID publicId) {
        return teamMembers.findByPublicId(publicId);
    }

    @Override
    @Transactional
    public com.simbio.core.content.domain.TeamMemberEntity saveTeamMember(
            com.simbio.core.content.domain.TeamMemberEntity member) {
        return teamMembers.save(member);
    }

    @Override
    public List<com.simbio.core.content.domain.TeamMemberEntity> findActiveTeamMembers() {
        return teamMembers.findAllByActiveTrueOrderByPositionAsc();
    }

    @Override
    public boolean isPublishedTeamPhoto(UUID filePublicId) {
        return teamMemberTranslations.existsInActiveMember(filePublicId);
    }
}
