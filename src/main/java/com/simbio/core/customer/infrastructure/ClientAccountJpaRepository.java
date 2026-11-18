package com.simbio.core.customer.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.customer.domain.ClientAccountEntity;

interface ClientAccountJpaRepository extends JpaRepository<ClientAccountEntity, Long> {

    @Query("""
            select distinct account from ClientAccountEntity account
            left join AccountMemberEntity member on member.account = account
            where (:status is null or account.status = :status)
              and (:search is null or lower(account.displayName) like lower(concat('%', :search, '%'))
                   or cast(account.publicId as string) like concat('%', :search, '%'))
            """)
    Page<ClientAccountEntity> search(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            select account from ClientAccountEntity account
            join AccountMemberEntity member on member.account = account
            where member.userPublicId = :userId
              and member.roleCode = 'OWNER'
              and member.statusCode = 'ACTIVE'
              and account.status = 'ACTIVE'
            """)
    Optional<ClientAccountEntity> findOwnedBy(@Param("userId") UUID userPublicId);

    @Query("""
            select account from ClientAccountEntity account
            join AccountMemberEntity member on member.account = account
            where account.publicId = :accountId
              and member.userPublicId = :userId
              and member.roleCode = 'OWNER'
              and member.statusCode = 'ACTIVE'
              and account.status = 'ACTIVE'
            """)
    Optional<ClientAccountEntity> findOwnedAccountBy(
            @Param("accountId") UUID accountPublicId,
            @Param("userId") UUID ownerUserPublicId);
}
