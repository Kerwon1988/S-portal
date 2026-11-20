package com.simbio.core.request.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;
import java.math.BigDecimal;
import java.time.Instant;
import com.simbio.core.request.domain.RequestAssignmentEntity;
import com.simbio.core.request.domain.RequestAttachmentEntity;
import com.simbio.core.request.domain.RequestEntity;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;

public interface RequestStore {
    RequestEntity save(RequestEntity request);
    Optional<RequestEntity> findByPublicId(UUID publicId);
    Optional<RequestEntity> findById(Long id);
    Optional<RequestEntity> findByReference(String reference);
    Page<RequestEntity> searchOwned(UUID ownerAccountId, RequestType type, RequestStatus status,
            String reference, java.time.Instant createdFrom, java.time.Instant createdBefore, Pageable pageable);
    RequestStatusHistoryEntity saveHistory(RequestStatusHistoryEntity history);
    List<RequestStatusHistoryEntity> history(Long requestId);
    RequestAttachmentEntity saveAttachment(RequestAttachmentEntity attachment);
    List<RequestAttachmentEntity> attachments(Long requestId);
    Optional<RequestAttachmentEntity> findAttachment(Long requestId, UUID filePublicId);
    boolean categoryExists(String category);
    long attachmentsCount(Long requestId);
    Page<RequestEntity> searchStaff(RequestType type, RequestStatus status, String category,
            UUID assignedTo, Instant createdFrom, Instant createdBefore, BigDecimal amountFrom,
            BigDecimal amountTo, String search, Pageable pageable);
    List<RequestEntity> openForStaff(UUID staffUserId, int limit);
    long countUnassigned();
    long countAssignedTo(UUID staffUserId);
    long countStatus(RequestStatus status);
    RequestAssignmentEntity saveAssignment(RequestAssignmentEntity assignment);
    List<RequestAssignmentEntity> assignments(Long requestId);
    void replaceAssignments(Long requestId, List<RequestAssignmentEntity> assignments);
    boolean isAssigned(Long requestId, UUID staffUserId);
}
