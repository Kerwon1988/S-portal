package com.simbio.core.customer.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.customer.application.ClientAccountStore;
import com.simbio.core.customer.application.ClientInvitationStore;
import com.simbio.core.customer.domain.AccountMemberEntity;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.ClientInvitationEntity;
import com.simbio.core.customer.domain.ClientAccountType;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaClientAccountStore.class, JpaClientInvitationStore.class})
class CustomerPersistenceH2IT {

    @Autowired
    private ClientAccountStore store;

    @Autowired
    private ClientInvitationStore invitations;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("TC-CUS-001: H2 stores the owner link using an identity public ID")
    void persistsAccountAndOwnerWithoutCrossModuleForeignKey() {
        UUID ownerId = UUID.randomUUID();
        ClientAccountEntity account = new ClientAccountEntity(UUID.randomUUID(), ClientAccountType.COMPANY,
                "Synthetic company", Instant.parse("2026-11-18T08:00:00Z"));
        store.save(account);
        store.save(new AccountMemberEntity(account, ownerId, "OWNER", "ACTIVE", account.getCreatedAt()));
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findOwnedBy(ownerId)).get()
                .satisfies(found -> {
                    assertThat(found.getAccountType()).isEqualTo(ClientAccountType.COMPANY);
                    assertThat(found.getDisplayName()).isEqualTo("Synthetic company");
                });
    }

    @Test
    @DisplayName("TC-CUS-001: a second active OWNER is rejected without replacing the first")
    void rejectsSecondActiveOwnerForTheSameAccount() {
        ClientAccountEntity account = new ClientAccountEntity(UUID.randomUUID(), ClientAccountType.INDIVIDUAL,
                "Synthetic account", Instant.parse("2026-11-18T08:00:00Z"));
        store.save(account);
        store.save(new AccountMemberEntity(account, UUID.randomUUID(), "OWNER", "ACTIVE", account.getCreatedAt()));
        entityManager.flush();

        assertThatThrownBy(() -> {
            store.save(new AccountMemberEntity(account, UUID.randomUUID(), "OWNER", "ACTIVE",
                    account.getCreatedAt()));
            entityManager.flush();
        }).isInstanceOfAny(ConstraintViolationException.class,
                org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("TC-CUS-004: H2 persists the ciphertext field and country for the owner")
    void persistsEncryptedTaxIdentifierAndCountryForTheOwner() {
        UUID ownerId = UUID.randomUUID();
        ClientAccountEntity account = new ClientAccountEntity(UUID.randomUUID(), ClientAccountType.COMPANY,
                "Synthetic company", Instant.parse("2026-11-18T08:00:00Z"));
        account.setTaxIdentifier("ES", "v1:synthetic-ciphertext");
        account.setPhoneCiphertext("v1:synthetic-phone-ciphertext");
        store.save(account);
        store.save(new AccountMemberEntity(account, ownerId, "OWNER", "ACTIVE", account.getCreatedAt()));
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findOwnedBy(ownerId)).get()
                .satisfies(found -> {
                    assertThat(found.getCountryCode()).isEqualTo("ES");
                    assertThat(found.getTaxIdCiphertext()).isEqualTo("v1:synthetic-ciphertext");
                    assertThat(found.getPhoneCiphertext()).isEqualTo("v1:synthetic-phone-ciphertext");
                });
    }

    @Test
    @DisplayName("TC-CUS-003: H2 persists opaque invitation correlation without recipient email")
    void persistsPendingInvitationByOpaqueReference() {
        UUID reference = UUID.randomUUID();
        ClientInvitationEntity invitation = new ClientInvitationEntity(
                reference, "COMPANY", "v1:synthetic-name-ciphertext", UUID.randomUUID(),
                Instant.parse("2026-11-18T08:00:00Z"));
        invitations.save(invitation);
        entityManager.flush();
        entityManager.clear();

        assertThat(invitations.findByReferenceForUpdate(reference)).get()
                .satisfies(found -> {
                    assertThat(found.getStatus()).isEqualTo("PENDING");
                    assertThat(found.getInvitationReference()).isEqualTo(reference);
                    assertThat(found.getAccountNameCiphertext()).isEqualTo("v1:synthetic-name-ciphertext");
                    assertThat(found.toString()).doesNotContain("@example.test", "Synthetic company");
                });
    }
}
