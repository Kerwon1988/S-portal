package com.simbio.core.request.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.RequestStore;
import com.simbio.core.request.domain.RequestAssignmentEntity;
import com.simbio.core.request.domain.RequestAttachmentEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;

@Repository
class JpaRequestStore implements RequestStore {
    private final RequestJpaRepository requests;
    private final RequestHistoryJpaRepository history;
    private final RequestAttachmentJpaRepository attachments;
    private final RequestCategoryJpaRepository categories;
    private final RequestAssignmentJpaRepository assignments;

    JpaRequestStore(RequestJpaRepository requests, RequestHistoryJpaRepository history,
            RequestAttachmentJpaRepository attachments, RequestCategoryJpaRepository categories,
            RequestAssignmentJpaRepository assignments) {
        this.requests = requests;
        this.history = history;
        this.attachments = attachments;
        this.categories = categories;
        this.assignments = assignments;
    }

    @Override public RequestEntity save(RequestEntity request) { return requests.save(request); }
    @Override public Optional<RequestEntity> findByPublicId(UUID publicId) { return requests.findByPublicId(publicId); }
    @Override public Optional<RequestEntity> findById(Long id) { return requests.findById(id); }
    @Override public Optional<RequestEntity> findByReference(String reference) {
        return requests.findByReferenceCode(reference);
    }
    @Override public Page<RequestEntity> searchOwned(UUID ownerAccountId, RequestType type,
            RequestStatus status, String reference, java.time.Instant createdFrom,
            java.time.Instant createdBefore, Pageable pageable) {
        return requests.searchOwned(ownerAccountId, type, status,
                reference == null ? "" : reference.trim(), createdFrom, createdBefore, pageable);
    }
    @Override public RequestStatusHistoryEntity saveHistory(RequestStatusHistoryEntity value) { return history.save(value); }
    @Override public List<RequestStatusHistoryEntity> history(Long requestId) { return history.findAllByRequestIdOrderByCreatedAtAsc(requestId); }
    @Override public RequestAttachmentEntity saveAttachment(RequestAttachmentEntity value) { return attachments.save(value); }
    @Override public List<RequestAttachmentEntity> attachments(Long requestId) { return attachments.findAllByRequestIdOrderByCreatedAtAsc(requestId); }
    @Override public Optional<RequestAttachmentEntity> findAttachment(Long requestId, UUID filePublicId) {
        return attachments.findByRequestIdAndFilePublicId(requestId, filePublicId);
    }
    @Override public boolean categoryExists(String category) { return categories.existsByCodeAndActiveTrue(category); }
    @Override public long attachmentsCount(Long requestId) { return attachments.countByRequestId(requestId); }
    @Override public Page<RequestEntity> searchStaff(com.simbio.core.request.api.RequestType type,
            com.simbio.core.request.api.RequestStatus status, String category, UUID assignedTo,
            java.time.Instant createdFrom, java.time.Instant createdBefore,
            java.math.BigDecimal amountFrom, java.math.BigDecimal amountTo, String search, Pageable pageable) {
        return requests.searchStaff(type, status, category == null ? "" : category.trim(), assignedTo,
                createdFrom, createdBefore, amountFrom, amountTo, search == null ? "" : search.trim(), pageable);
    }
    @Override public List<RequestEntity> openForStaff(UUID staffUserId, int limit) {
        return requests.findOpenAssignedTo(staffUserId, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    @Override public long countUnassigned() { return requests.countUnassigned(); }
    @Override public long countAssignedTo(UUID staffUserId) { return requests.countAssignedTo(staffUserId); }
    @Override public long countStatus(com.simbio.core.request.api.RequestStatus status) { return requests.countByStatus(status); }
    @Override public RequestAssignmentEntity saveAssignment(RequestAssignmentEntity assignment) { return assignments.save(assignment); }
    @Override public List<RequestAssignmentEntity> assignments(Long requestId) { return assignments.findAllByRequestIdOrderByCreatedAtAsc(requestId); }
    @Override public void replaceAssignments(Long requestId, List<RequestAssignmentEntity> values) {
        assignments.deleteByRequestId(requestId);
        assignments.saveAll(values);
    }
    @Override public boolean isAssigned(Long requestId, UUID staffUserId) {
        return assignments.existsByRequestIdAndStaffUserId(requestId, staffUserId);
    }
}
