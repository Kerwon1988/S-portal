package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;

public interface UserAccountStore {

    Optional<UserAccountEntity> findByEmail(String normalizedEmail);

    boolean existsAdmin();

    UserAccountEntity createAdmin(String normalizedEmail, String passwordHash, String locale);

    UserAccountEntity createUser(String normalizedEmail, String passwordHash, String locale, String roleCode,
            String displayName);

    UserAccountEntity createUnverifiedUser(
            String normalizedEmail, String passwordHash, String locale, String roleCode, String displayName);

    UserAccountEntity save(UserAccountEntity account);

    Optional<UserAccountEntity> findByPublicId(UUID publicId);

    RoleEntity findRole(String code);

    List<UserAccountEntity> lockActiveAdmins();

    Page<UserAccountEntity> search(
            String search,
            String role,
            String status,
            String locale,
            Instant createdFrom,
            Instant createdBefore,
            Pageable pageable);
}
