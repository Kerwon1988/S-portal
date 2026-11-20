package com.simbio.core.request.infrastructure;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.RequestEntity;

interface RequestJpaRepository extends JpaRepository<RequestEntity, Long> {
    Optional<RequestEntity> findByPublicId(UUID publicId);
    Optional<RequestEntity> findByReferenceCode(String referenceCode);

    @Query("""
            select r from RequestEntity r
            where r.ownerAccountId = :ownerAccountId
              and (:type is null or r.type = :type)
              and (:status is null or r.status = :status)
              and (:reference = '' or lower(r.referenceCode) like lower(concat('%', :reference, '%')))
              and (:createdFrom is null or r.createdAt >= :createdFrom)
              and (:createdBefore is null or r.createdAt < :createdBefore)
            """)
    Page<RequestEntity> searchOwned(
            @Param("ownerAccountId") UUID ownerAccountId,
            @Param("type") RequestType type,
            @Param("status") RequestStatus status,
            @Param("reference") String reference,
            @Param("createdFrom") java.time.Instant createdFrom,
            @Param("createdBefore") java.time.Instant createdBefore,
            Pageable pageable);

    @Query("""
            select r from RequestEntity r
            where (:type is null or r.type = :type)
              and (:status is null or r.status = :status)
              and (:category = '' or r.category = :category)
              and (:assignedTo is null or exists (
                    select a.id from com.simbio.core.request.domain.RequestAssignmentEntity a
                    where a.requestId = r.id and a.staffUserId = :assignedTo))
              and (:createdFrom is null or r.createdAt >= :createdFrom)
              and (:createdBefore is null or r.createdAt < :createdBefore)
              and (:amountFrom is null or r.estimatedBudget >= :amountFrom)
              and (:amountTo is null or r.estimatedBudget <= :amountTo)
              and (:search = '' or lower(r.referenceCode) like lower(concat('%', :search, '%'))
                   or lower(r.title) like lower(concat('%', :search, '%')))
            """)
    Page<RequestEntity> searchStaff(@Param("type") RequestType type,
            @Param("status") RequestStatus status, @Param("category") String category,
            @Param("assignedTo") UUID assignedTo, @Param("createdFrom") Instant createdFrom,
            @Param("createdBefore") Instant createdBefore, @Param("amountFrom") BigDecimal amountFrom,
            @Param("amountTo") BigDecimal amountTo, @Param("search") String search, Pageable pageable);

    @Query("""
            select r from RequestEntity r
            where r.status not in (com.simbio.core.request.api.RequestStatus.CANCELADA,
              com.simbio.core.request.api.RequestStatus.CERRADA,
              com.simbio.core.request.api.RequestStatus.DESCARTADA,
              com.simbio.core.request.api.RequestStatus.DUPLICADA)
              and exists (select a.id from com.simbio.core.request.domain.RequestAssignmentEntity a
                          where a.requestId = r.id and a.staffUserId = :staffUserId)
            order by r.createdAt asc
            """)
    List<RequestEntity> findOpenAssignedTo(@Param("staffUserId") UUID staffUserId, Pageable pageable);

    @Query("""
            select count(r) from RequestEntity r
            where r.status not in (com.simbio.core.request.api.RequestStatus.CANCELADA,
              com.simbio.core.request.api.RequestStatus.CERRADA,
              com.simbio.core.request.api.RequestStatus.DESCARTADA,
              com.simbio.core.request.api.RequestStatus.DUPLICADA)
              and not exists (select a.id from com.simbio.core.request.domain.RequestAssignmentEntity a
                              where a.requestId = r.id and a.responsible = true)
            """)
    long countUnassigned();

    @Query("""
            select count(r) from RequestEntity r
            where r.status not in (com.simbio.core.request.api.RequestStatus.CANCELADA,
              com.simbio.core.request.api.RequestStatus.CERRADA,
              com.simbio.core.request.api.RequestStatus.DESCARTADA,
              com.simbio.core.request.api.RequestStatus.DUPLICADA)
              and exists (select a.id from com.simbio.core.request.domain.RequestAssignmentEntity a
                          where a.requestId = r.id and a.staffUserId = :staffUserId)
            """)
    long countAssignedTo(@Param("staffUserId") UUID staffUserId);

    long countByStatus(RequestStatus status);
}
