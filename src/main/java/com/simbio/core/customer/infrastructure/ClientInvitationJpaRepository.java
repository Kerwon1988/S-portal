package com.simbio.core.customer.infrastructure;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.customer.domain.ClientInvitationEntity;

interface ClientInvitationJpaRepository extends JpaRepository<ClientInvitationEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from ClientInvitationEntity invitation "
            + "where invitation.invitationReference = :reference")
    Optional<ClientInvitationEntity> findByReferenceForUpdate(@Param("reference") UUID invitationReference);
}
