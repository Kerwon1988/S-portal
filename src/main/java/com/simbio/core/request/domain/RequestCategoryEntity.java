package com.simbio.core.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_category", schema = "request")
public class RequestCategoryEntity {
    @Id
    @Column(name = "code", length = 40)
    private String code;
    @Column(name = "active", nullable = false)
    private boolean active;
}
