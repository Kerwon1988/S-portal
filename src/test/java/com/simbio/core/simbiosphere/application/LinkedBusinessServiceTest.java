package com.simbio.core.simbiosphere.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;

class LinkedBusinessServiceTest {
    @Test
    void listsOnlyBusinessesBelongingToResolvedOwnerAccount() {
        UUID owner = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        CustomerDirectory customers = mock(CustomerDirectory.class);
        LinkedBusinessStore store = mock(LinkedBusinessStore.class);
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(new CustomerAccountSummary(
                account, "COMPANY", "Synthetic", "ACTIVE", Instant.now())));
        AccountLinkEntity link = new AccountLinkEntity(UUID.randomUUID(), account,
                "SIMBIOSPHERE", "external-account", Instant.now());
        LinkedBusinessEntity business = new LinkedBusinessEntity(UUID.randomUUID(), link,
                "20", "Synthetic shop", "SERVICES", "synthetic-shop", Instant.now());
        when(store.findByClientAccount(account)).thenReturn(List.of(business));

        LinkedBusinessService service = new LinkedBusinessService(customers, store);
        assertThat(service.findOwnedBy(owner)).singleElement()
                .satisfies(value -> assertThat(value.clientAccountPublicId()).isEqualTo(account));
    }

    @Test
    void rejectsManipulatedForeignBusinessPublicId() {
        UUID owner = UUID.randomUUID();
        CustomerDirectory customers = mock(CustomerDirectory.class);
        LinkedBusinessStore store = mock(LinkedBusinessStore.class);
        when(customers.findOwnedBy(owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new LinkedBusinessService(customers, store)
                .requireOwnedBy(owner, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not owned");
    }
}
