package com.simbio.core.identity.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

public final class PortalUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final Set<Permission> permissions;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public PortalUserPrincipal(UserAccountEntity account) {
        userId = account.getPublicId();
        email = account.getEmailNormalized();
        passwordHash = account.getPasswordHash();
        enabled = account.isEnabled();
        accountNonLocked = !account.isLockedAt(java.time.Instant.now());
        Set<Role> roles = account.getRoles().stream()
                .map(roleEntity -> parseRole(roleEntity.getCode()))
                .collect(Collectors.toUnmodifiableSet());
        role = roles.contains(Role.ADMIN) ? Role.ADMIN
                : roles.contains(Role.EMPLOYEE) ? Role.EMPLOYEE
                : roles.contains(Role.CLIENT) ? Role.CLIENT
                : throwMissingRole();
        permissions = account.getRoles().stream()
                .flatMap(roleEntity -> roleEntity.getPermissions().stream())
                .map(permissionEntity -> parsePermission(permissionEntity.getCode()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Role parseRole(String code) {
        try {
            return Role.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Unknown identity role", exception);
        }
    }

    private static Role throwMissingRole() {
        throw new IllegalStateException("User account has no supported role");
    }

    private static Permission parsePermission(String code) {
        try {
            return Permission.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Unknown identity permission", exception);
        }
    }

    public AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(userId, role, permissions);
    }

    UUID userId() {
        return userId;
    }

    public UUID publicUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
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
    public boolean equals(Object other) {
        return other instanceof PortalUserPrincipal principal && userId.equals(principal.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
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
