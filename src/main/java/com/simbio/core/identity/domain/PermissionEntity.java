package com.simbio.core.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permission", schema = "identity")
public class PermissionEntity {

    @Id
    @Column(name = "code", nullable = false, length = 64)
    private String code;
    @Column(name = "description", nullable = false, length = 128)
    private String description;

    protected PermissionEntity() {
    }

    public PermissionEntity(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
