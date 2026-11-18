package com.simbio.core.identity.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.identity.domain.UserAccountEntity;

interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByEmailNormalized(String normalizedEmail);

    boolean existsByRoles_Code(String code);

    Optional<UserAccountEntity> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct account from UserAccountEntity account
            join account.roles role
            where account.status = 'ACTIVE' and role.code = 'ADMIN'
            order by account.id
            """)
    List<UserAccountEntity> lockActiveAdmins();

    @Query("""
            select distinct account from UserAccountEntity account
            join account.roles role
            where (:role is null or role.code = :role
                   or (:role = 'STAFF' and role.code in ('ADMIN', 'EMPLOYEE')))
              and (:status is null or account.status = :status)
              and (:locale is null or account.preferredLocale = :locale)
              and (:createdFrom is null or account.createdAt >= :createdFrom)
              and (:createdBefore is null or account.createdAt < :createdBefore)
              and (:search is null or lower(account.emailNormalized) like lower(concat('%', :search, '%'))
                   or lower(coalesce(account.displayName, '')) like lower(concat('%', :search, '%'))
                   or cast(account.publicId as string) like concat('%', :search, '%'))
            """)
    Page<UserAccountEntity> search(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") String status,
            @Param("locale") String locale,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdBefore") Instant createdBefore,
            Pageable pageable);
}
