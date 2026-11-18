package com.simbio.core.content.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_member", schema = "content")
public class TeamMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "position_no", nullable = false)
    private int position;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "materials_approved", nullable = false)
    private boolean materialsApproved;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMemberTranslationEntity> translations = new ArrayList<>();

    protected TeamMemberEntity() {
    }

    public TeamMemberEntity(UUID publicId, int position, boolean active, boolean materialsApproved) {
        this.publicId = publicId;
        this.position = position;
        this.active = active;
        this.materialsApproved = materialsApproved;
    }

    public TeamMemberTranslationEntity translation(String locale) {
        return translations.stream()
                .filter(value -> value.getLocale().equals(locale))
                .findFirst()
                .orElseGet(() -> {
                    TeamMemberTranslationEntity translation = new TeamMemberTranslationEntity(this, locale);
                    translations.add(translation);
                    return translation;
                });
    }

    public void saveDraft(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Team member position must not be negative");
        }
        this.position = position;
        this.active = false;
        this.materialsApproved = false;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public int getPosition() {
        return position;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMaterialsApproved() {
        return materialsApproved;
    }

    public List<TeamMemberTranslationEntity> getTranslations() {
        return List.copyOf(translations);
    }
}
