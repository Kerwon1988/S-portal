package com.simbio.core.identity.infrastructure;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.IdentityInitializer;
import com.simbio.core.identity.api.Permission;
import com.simbio.core.identity.api.Role;
import com.simbio.core.identity.domain.PermissionEntity;
import com.simbio.core.identity.domain.RoleEntity;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentitySeed implements ApplicationRunner, IdentityInitializer {

    private static final Map<Role, String> ROLE_DESCRIPTIONS = Map.of(
            Role.ADMIN, "Administrator",
            Role.EMPLOYEE, "Employee",
            Role.CLIENT, "Client");

    private final RoleJpaRepository roles;
    private final PermissionJpaRepository permissions;

    IdentitySeed(RoleJpaRepository roles, PermissionJpaRepository permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        seed();
    }

    @Override
    @Transactional
    public void seed() {
        for (Role role : Role.values()) {
            roles.findById(role.name()).orElseGet(() -> roles.save(new RoleEntity(
                    role.name(), ROLE_DESCRIPTIONS.get(role))));
        }
        for (Permission permission : Permission.values()) {
            permissions.findById(permission.name()).orElseGet(() ->
                    permissions.save(new PermissionEntity(permission.name(), permission.name())));
        }
        RoleEntity admin = roles.findById(Role.ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("ADMIN role seed failed"));
        for (Permission permission : Permission.values()) {
            PermissionEntity entity = permissions.findById(permission.name())
                    .orElseThrow(() -> new IllegalStateException("Permission seed failed"));
            admin.getPermissions().add(entity);
        }
        roles.save(admin);
    }
}
