package com.simbio.core.content.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.content.domain.MediaAssetEntity;
import com.simbio.core.content.domain.LegalDocumentVersionEntity;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.content.domain.PageVersionEntity;
import com.simbio.core.content.domain.SlugRedirectEntity;
import com.simbio.core.content.domain.TeamMemberEntity;

public interface ContentStore {

    Optional<PageEntity> findPageByPublicId(UUID publicId);

    Optional<PageEntity> findPageByKey(String pageKey);

    Optional<PageTranslationEntity> findTranslation(String pageKey, String locale);

    Optional<PageTranslationEntity> findTranslationByPublicId(UUID publicId);

    Optional<PageTranslationEntity> findByLocaleAndSlug(String locale, String slug);

    Optional<PageTranslationEntity> findPublishedByLocaleAndSlug(String locale, String slug);

    Optional<PageTranslationEntity> findAnyPublishedBySlug(String slug);

    List<PageTranslationEntity> findAllTranslations();

    List<PageTranslationEntity> findPublishedTranslations();

    PageEntity savePage(PageEntity page);

    PageTranslationEntity saveTranslation(PageTranslationEntity translation);

    PageVersionEntity saveVersion(PageVersionEntity version);

    List<PageVersionEntity> findVersions(UUID translationPublicId);

    Optional<PageVersionEntity> findVersion(UUID translationPublicId, int version);

    Optional<SlugRedirectEntity> findRedirect(String locale, String oldSlug);

    SlugRedirectEntity saveRedirect(SlugRedirectEntity redirect);

    Optional<MediaAssetEntity> findMedia(UUID filePublicId);

    List<MediaAssetEntity> findAllMedia();

    MediaAssetEntity saveMedia(MediaAssetEntity media);

    boolean isPublishedMedia(UUID filePublicId);

    LegalDocumentVersionEntity saveLegalDocument(LegalDocumentVersionEntity document);

    List<LegalDocumentVersionEntity> findLegalDocuments();

    Optional<LegalDocumentVersionEntity> findLatestLegalDocument(String documentType, String locale);

    Optional<LegalDocumentVersionEntity> findCurrentLegalDocument(
            String documentType, String locale, java.time.Instant effectiveAt);

    List<TeamMemberEntity> findTeamMembers();

    Optional<TeamMemberEntity> findTeamMember(UUID publicId);

    TeamMemberEntity saveTeamMember(TeamMemberEntity member);

    List<TeamMemberEntity> findActiveTeamMembers();

    boolean isPublishedTeamPhoto(UUID filePublicId);
}
