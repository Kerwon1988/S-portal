package com.simbio.core.identity.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.UserAccountStore;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;

@Repository
class JpaUserAccountStore implements UserAccountStore {

    private final UserAccountJpaRepository users;
    private final RoleJpaRepository roles;

    JpaUserAccountStore(UserAccountJpaRepository users, RoleJpaRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccountEntity> findByEmail(String normalizedEmail) {
        return users.findByEmailNormalized(normalizeEmail(normalizedEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAdmin() {
        return users.existsByRoles_Code("ADMIN");
    }

    @Override
    @Transactional
    public UserAccountEntity createAdmin(String normalizedEmail, String passwordHash, String locale) {
        RoleEntity admin = findRole("ADMIN");
        return users.save(new UserAccountEntity(UUID.randomUUID(), normalizeEmail(normalizedEmail),
                passwordHash, locale, Instant.now(Clock.systemUTC()), admin));
    }

    @Override
    @Transactional
    public UserAccountEntity save(UserAccountEntity account) {
        return users.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccountEntity> findByPublicId(UUID publicId) {
        return users.findByPublicId(publicId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleEntity findRole(String code) {
        return roles.findById(code).orElseThrow(() -> new IllegalStateException("Required role is not seeded"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
