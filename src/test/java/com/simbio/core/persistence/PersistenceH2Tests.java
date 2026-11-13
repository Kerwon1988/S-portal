package com.simbio.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceH2Tests {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReadsAnEntityUsingGeneratedH2Schema() {
        TestRecord record = new TestRecord("h2-persistence");
        entityManager.persist(record);
        entityManager.flush();
        entityManager.clear();

        TestRecord loaded = entityManager.find(TestRecord.class, record.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getPayload()).isEqualTo("h2-persistence");
    }
}
