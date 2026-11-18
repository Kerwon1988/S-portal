package com.simbio.core.staff.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "staff_profile", schema = "staff")
public class StaffProfileEntity {

    @Id
    @Column(name = "user_public_id")
    private UUID userPublicId;
    @Column(name = "job_title", length = 120)
    private String jobTitle;
    @Column(name = "employment_start_date", nullable = false)
    private LocalDate employmentStartDate;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version
    private long version;

    protected StaffProfileEntity() {
    }

    public StaffProfileEntity(UUID userPublicId, LocalDate employmentStartDate, Instant createdAt) {
        if (userPublicId == null || employmentStartDate == null || createdAt == null) {
            throw new IllegalArgumentException("Worker identity and creation time are required");
        }
        this.userPublicId = userPublicId;
        this.employmentStartDate = employmentStartDate;
        this.status = "ACTIVE";
        this.createdAt = createdAt;
    }

    public void update(String jobTitle, String status) {
        if (jobTitle != null && jobTitle.length() > 120) {
            throw new IllegalArgumentException("Job title is too long");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("Unsupported worker status");
        }
        this.jobTitle = jobTitle == null || jobTitle.isBlank() ? null : jobTitle.trim();
        this.status = status;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public LocalDate getEmploymentStartDate() {
        return employmentStartDate;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
