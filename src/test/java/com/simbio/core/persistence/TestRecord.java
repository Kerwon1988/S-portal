package com.simbio.core.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "persistence_test_record")
public class TestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payload;

    protected TestRecord() {
    }

    public TestRecord(String payload) {
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }
}
