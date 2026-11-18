package com.simbio.core.customer.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.customer.domain.AccountMemberEntity;
import com.simbio.core.customer.domain.ClientAccountEntity;

public interface ClientAccountStore {

    ClientAccountEntity save(ClientAccountEntity account);

    AccountMemberEntity save(AccountMemberEntity member);

    Page<ClientAccountEntity> search(String search, String status, Pageable pageable);

    Optional<ClientAccountEntity> findOwnedBy(UUID userPublicId);

    Optional<ClientAccountEntity> findOwnedAccountBy(UUID accountPublicId, UUID ownerUserPublicId);
}
