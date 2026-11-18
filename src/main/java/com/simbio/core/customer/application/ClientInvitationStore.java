package com.simbio.core.customer.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.customer.domain.ClientInvitationEntity;

public interface ClientInvitationStore {

    ClientInvitationEntity save(ClientInvitationEntity invitation);

    Optional<ClientInvitationEntity> findByReferenceForUpdate(UUID invitationReference);
}
