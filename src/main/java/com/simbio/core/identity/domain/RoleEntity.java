package com.simbio.core.identity.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "role", schema = "identity")
public class RoleEntity {

    @Id
    @Column(name = "code", nullable = false, length = 32)
    private String code;
    @Column(name = "description", nullable = false, length = 128)
    private String description;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permission", schema = "identity",
            joinColumns = @JoinColumn(name = "role_code"),
            inverseJoinColumns = @JoinColumn(name = "permission_code"))
    private Set<PermissionEntity> permissions = new HashSet<>();

    protected RoleEntity() {
    }

    public RoleEntity(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public Set<PermissionEntity> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoleEntity role && Objects.equals(code, role.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
