package com.simbio.core.identity.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;

public interface UserAccountStore {

    Optional<UserAccountEntity> findByEmail(String normalizedEmail);

    boolean existsAdmin();

    UserAccountEntity createAdmin(String normalizedEmail, String passwordHash, String locale);

    UserAccountEntity save(UserAccountEntity account);

    Optional<UserAccountEntity> findByPublicId(UUID publicId);

    RoleEntity findRole(String code);
}
