package com.simbio.core.simbiosphere.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.simbiosphere.domain.AccountLinkEntity;

interface AccountLinkJpaRepository extends JpaRepository<AccountLinkEntity, Long> {
}
