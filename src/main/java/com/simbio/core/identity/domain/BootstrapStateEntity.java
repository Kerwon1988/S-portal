package com.simbio.core.identity.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bootstrap_state", schema = "identity")
public class BootstrapStateEntity {

    @Id
    private Byte id;
    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    protected BootstrapStateEntity() {
    }

    public BootstrapStateEntity(Instant consumedAt) {
        this.id = 1;
        this.consumedAt = consumedAt;
    }
}
