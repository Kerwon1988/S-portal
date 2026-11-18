package com.simbio.core.customer.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.customer.application.ClientAccountStore;
import com.simbio.core.customer.domain.AccountMemberEntity;
import com.simbio.core.customer.domain.ClientAccountEntity;

@Repository
class JpaClientAccountStore implements ClientAccountStore {

    private final ClientAccountJpaRepository accounts;
    private final AccountMemberJpaRepository members;

    JpaClientAccountStore(ClientAccountJpaRepository accounts, AccountMemberJpaRepository members) {
        this.accounts = accounts;
        this.members = members;
    }

    @Override
    @Transactional
    public ClientAccountEntity save(ClientAccountEntity account) {
        return accounts.save(account);
    }

    @Override
    @Transactional
    public AccountMemberEntity save(AccountMemberEntity member) {
        return members.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientAccountEntity> search(String search, String status, Pageable pageable) {
        return accounts.search(search, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientAccountEntity> findOwnedBy(UUID userPublicId) {
        return accounts.findOwnedBy(userPublicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientAccountEntity> findOwnedAccountBy(UUID accountPublicId, UUID ownerUserPublicId) {
        return accounts.findOwnedAccountBy(accountPublicId, ownerUserPublicId);
    }
}
