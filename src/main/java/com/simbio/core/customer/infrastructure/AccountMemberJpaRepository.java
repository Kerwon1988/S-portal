package com.simbio.core.customer.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.customer.domain.AccountMemberEntity;

interface AccountMemberJpaRepository extends JpaRepository<AccountMemberEntity, Long> {
}
