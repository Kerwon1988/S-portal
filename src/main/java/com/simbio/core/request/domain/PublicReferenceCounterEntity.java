package com.simbio.core.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reference_counter", schema = "request")
public class PublicReferenceCounterEntity {
    @Id
    @Column(name = "business_year")
    private int businessYear;
    @Column(name = "last_number", nullable = false)
    private long lastNumber;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PublicReferenceCounterEntity() {
    }

    public PublicReferenceCounterEntity(int businessYear) {
        this.businessYear = businessYear;
    }

    public long next() {
        return ++lastNumber;
    }
}
