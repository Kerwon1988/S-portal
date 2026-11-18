package com.simbio.core.customer.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.customer.application.ClientInvitationStore;
import com.simbio.core.customer.domain.ClientInvitationEntity;

@Repository
class JpaClientInvitationStore implements ClientInvitationStore {

    private final ClientInvitationJpaRepository invitations;

    JpaClientInvitationStore(ClientInvitationJpaRepository invitations) {
        this.invitations = invitations;
    }

    @Override
    @Transactional
    public ClientInvitationEntity save(ClientInvitationEntity invitation) {
        return invitations.save(invitation);
    }

    @Override
    @Transactional
    public Optional<ClientInvitationEntity> findByReferenceForUpdate(UUID invitationReference) {
        return invitations.findByReferenceForUpdate(invitationReference);
    }
}
