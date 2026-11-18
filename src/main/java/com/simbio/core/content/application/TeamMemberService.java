package com.simbio.core.content.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.content.domain.TeamMemberEntity;
import com.simbio.core.content.domain.TeamMemberTranslationEntity;
import com.simbio.core.content.api.TeamMemberView;
import com.simbio.core.content.api.TeamMembersReader;
import com.simbio.core.files.api.CleanFileMetadataReader;
import com.simbio.core.files.api.ImageVariants;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamMemberService implements TeamMembersReader {

    private final ContentStore store;
    private final ContentSanitizer sanitizer;
    private final CleanFileMetadataReader cleanFileMetadataReader;
    private final ImageVariants imageVariants;
    private final AuditRecorder auditRecorder;

    public TeamMemberService(ContentStore store, ContentSanitizer sanitizer,
            CleanFileMetadataReader cleanFileMetadataReader, ImageVariants imageVariants,
            AuditRecorder auditRecorder) {
        this.store = Objects.requireNonNull(store);
        this.sanitizer = Objects.requireNonNull(sanitizer);
        this.cleanFileMetadataReader = Objects.requireNonNull(cleanFileMetadataReader);
        this.imageVariants = Objects.requireNonNull(imageVariants);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
    }

    @Transactional
    public TeamMemberDraftView save(AuthenticatedUser actor, UUID publicId, String locale, int position,
            String displayName, String roleTitle, String bioMarkdown, UUID photoPublicId, String photoAlt,
            String profileUrl) {
        requireEditor(actor);
        if (!("es".equals(locale) || "en".equals(locale)) || position < 0) {
            throw new ContentValidationException("Invalid team member locale or position");
        }
        String name = required(displayName, 120, "team member name");
        String role = required(roleTitle, 120, "team member role");
        if (bioMarkdown == null || bioMarkdown.length() > 4000) {
            throw new ContentValidationException("Invalid team member biography");
        }
        sanitizer.sanitize(bioMarkdown);
        String safeAlt = photoPublicId == null ? "" : required(photoAlt, 500, "photo alternative text");
        if (photoPublicId != null && (store.findMedia(photoPublicId).isEmpty()
                || cleanFileMetadataReader.findClean(photoPublicId).isEmpty())) {
            throw new ContentValidationException("Team photos must reference registered clean media");
        }
        String safeProfileUrl = profileUrl == null || profileUrl.isBlank() ? null : httpsUrl(profileUrl);
        TeamMemberEntity member = publicId == null
                ? new TeamMemberEntity(UUID.randomUUID(), position, false, false)
                : store.findTeamMember(publicId).orElseThrow(ContentNotFoundException::new);
        TeamMemberTranslationEntity translation = member.translation(locale);
        translation.edit(name, role, bioMarkdown, photoPublicId, safeAlt, safeProfileUrl);
        member.saveDraft(position);
        store.saveTeamMember(member);
        auditRecorder.record(new AuditRecord(actor.userId(), "CONTENT_TEAM_MEMBER_SAVED",
                "CONTENT_TEAM_MEMBER", member.getPublicId(), null));
        return draftView(member, translation);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDraftView> listForEditor(AuthenticatedUser actor) {
        requireEditor(actor);
        return store.findTeamMembers().stream()
                .flatMap(member -> member.getTranslations().stream().map(translation -> draftView(member, translation)))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<TeamMemberView> publicMembers(String locale) {
        if (!("es".equals(locale) || "en".equals(locale))) {
            throw new ContentValidationException("Only es and en locales are supported");
        }
        return store.findActiveTeamMembers().stream()
                .filter(TeamMemberEntity::isMaterialsApproved)
                .map(member -> member.getTranslations().stream()
                        .filter(translation -> translation.getLocale().equals(locale))
                        .findFirst()
                        .filter(translation -> translation.getPhotoPublicId() == null
                                || cleanFileMetadataReader.findClean(translation.getPhotoPublicId()).isPresent())
                        .map(translation -> toPublicView(translation, locale))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private TeamMemberView toPublicView(TeamMemberTranslationEntity translation, String locale) {
        UUID photoId = translation.getPhotoPublicId();
        String photoSrcSet = photoId == null ? "" : imageVariants.listVariants(photoId,
                requested -> requested.equals(photoId)).stream()
                .map(variant -> "/public/media/" + photoId + "/variants/" + variant.code()
                        + " " + variant.width() + "w")
                .collect(java.util.stream.Collectors.joining(", "));
        return new TeamMemberView(
                translation.getMember().getPublicId(),
                translation.getDisplayName(),
                translation.getRoleTitle(),
                sanitizer.sanitize(translation.getBioMarkdown()),
                photoId == null ? null : "/public/media/" + photoId,
                photoSrcSet,
                translation.getPhotoAlt(),
                translation.getProfileUrl());
    }

    private static TeamMemberDraftView draftView(TeamMemberEntity member, TeamMemberTranslationEntity translation) {
        return new TeamMemberDraftView(member.getPublicId(), member.getPosition(), member.isActive(),
                member.isMaterialsApproved(), translation.getLocale(), translation.getDisplayName(),
                translation.getRoleTitle(), translation.getProfileUrl());
    }

    private static String httpsUrl(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ContentValidationException("Team profile links must use HTTPS");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new ContentValidationException("Team profile URL is invalid");
        }
    }

    private static String required(String value, int maximumLength, String field) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new ContentValidationException("Invalid " + field);
        }
        return value.trim();
    }

    private static void requireEditor(AuthenticatedUser actor) {
        if (actor == null || !actor.hasPermission(Permission.CONTENT_EDIT)) {
            throw new ContentPermissionException();
        }
    }
}
