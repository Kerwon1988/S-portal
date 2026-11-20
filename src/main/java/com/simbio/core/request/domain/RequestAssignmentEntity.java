package com.simbio.core.request.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_assignment", schema = "request")
public class RequestAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @Column(name = "staff_user_id", nullable = false)
    private UUID staffUserId;
    @Column(name = "responsible", nullable = false)
    private boolean responsible;
    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequestAssignmentEntity() {
    }

    public RequestAssignmentEntity(Long requestId, UUID staffUserId, boolean responsible,
            UUID assignedBy, Instant createdAt) {
        this.requestId = Objects.requireNonNull(requestId);
        this.staffUserId = Objects.requireNonNull(staffUserId);
        this.responsible = responsible;
        this.assignedBy = Objects.requireNonNull(assignedBy);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getStaffUserId() { return staffUserId; }
    public boolean isResponsible() { return responsible; }
}
