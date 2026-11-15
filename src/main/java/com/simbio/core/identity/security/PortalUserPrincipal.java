package com.simbio.core.identity.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

final class PortalUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final Set<Permission> permissions;
    private final boolean enabled;
    private final boolean accountNonLocked;

    PortalUserPrincipal(UserAccountEntity account) {
        userId = account.getPublicId();
        email = account.getEmailNormalized();
        passwordHash = account.getPasswordHash();
        enabled = account.isEnabled();
        accountNonLocked = !account.isLockedAt(java.time.Instant.now());
        var roleNames = account.getRoles().stream().map(roleEntity -> roleEntity.getCode()).collect(java.util.stream.Collectors.toSet());
        role = roleNames.contains("ADMIN") ? Role.ADMIN
                : roleNames.contains("EMPLOYEE") ? Role.EMPLOYEE : Role.CLIENT;
        permissions = account.getRoles().stream()
                .flatMap(roleEntity -> roleEntity.getPermissions().stream())
                .map(permissionEntity -> Permission.valueOf(permissionEntity.getCode()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(userId, role, permissions);
    }

    UUID userId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorities = new java.util.ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.name())).forEach(authorities::add);
        return java.util.List.copyOf(authorities);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
