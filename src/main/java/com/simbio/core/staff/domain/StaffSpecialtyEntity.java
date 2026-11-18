package com.simbio.core.staff.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "staff_specialty", schema = "staff",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_staff_specialty_worker_name", columnNames = {"user_public_id", "name_key"}))
public class StaffSpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_public_id", nullable = false)
    private UUID userPublicId;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "name_key", nullable = false, length = 80)
    private String nameKey;

    protected StaffSpecialtyEntity() {
    }

    public StaffSpecialtyEntity(UUID userPublicId, String name) {
        if (userPublicId == null || name == null || name.isBlank() || name.trim().length() > 80) {
            throw new IllegalArgumentException("Invalid worker specialty");
        }
        this.userPublicId = userPublicId;
        this.name = name.trim();
        this.nameKey = name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String getName() {
        return name;
    }
}
