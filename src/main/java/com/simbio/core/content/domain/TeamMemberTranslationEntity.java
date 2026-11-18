package com.simbio.core.content.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "team_member_translation", schema = "content",
        uniqueConstraints = @UniqueConstraint(name = "uq_content_team_translation_member_locale",
                columnNames = {"member_id", "locale"}))
public class TeamMemberTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private TeamMemberEntity member;

    @Column(name = "locale", nullable = false, length = 2)
    private String locale;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "role_title", nullable = false, length = 120)
    private String roleTitle;

    @Column(name = "bio_markdown", nullable = false, length = 4000)
    private String bioMarkdown;

    @Column(name = "photo_public_id")
    private UUID photoPublicId;

    @Column(name = "photo_alt", nullable = false, length = 500)
    private String photoAlt;

    @Column(name = "profile_url", length = 512)
    private String profileUrl;

    protected TeamMemberTranslationEntity() {
    }

    TeamMemberTranslationEntity(TeamMemberEntity member, String locale) {
        this.member = member;
        this.locale = locale;
        this.displayName = "";
        this.roleTitle = "";
        this.bioMarkdown = "";
        this.photoAlt = "";
    }

    public void edit(String displayName, String roleTitle, String bioMarkdown,
            UUID photoPublicId, String photoAlt, String profileUrl) {
        this.displayName = displayName;
        this.roleTitle = roleTitle;
        this.bioMarkdown = bioMarkdown;
        this.photoPublicId = photoPublicId;
        this.photoAlt = photoAlt;
        this.profileUrl = profileUrl;
    }

    public String getLocale() {
        return locale;
    }

    public TeamMemberEntity getMember() {
        return member;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public String getBioMarkdown() {
        return bioMarkdown;
    }

    public UUID getPhotoPublicId() {
        return photoPublicId;
    }

    public String getPhotoAlt() {
        return photoAlt;
    }

    public String getProfileUrl() {
        return profileUrl;
    }
}
