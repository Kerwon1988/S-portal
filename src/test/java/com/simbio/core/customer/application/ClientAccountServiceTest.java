package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.domain.AccountMemberEntity;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.identity.api.IdentityUserLookup;
import com.simbio.core.shared.paging.PageQuery;

class ClientAccountServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-18T08:00:00Z");
    private final ClientAccountStore store = org.mockito.Mockito.mock(ClientAccountStore.class);
    private final IdentityUserLookup identityUsers = org.mockito.Mockito.mock(IdentityUserLookup.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ClientAccountService service = new ClientAccountService(store, identityUsers, auditRecorder,
            Clock.fixed(CREATED_AT, ZoneOffset.UTC));

    @Test
    void createsOneOwnerForAnExistingIdentityUser() {
        UUID ownerId = UUID.randomUUID();
        when(identityUsers.existsById(ownerId)).thenReturn(true);
        when(store.save(any(ClientAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.save(any(AccountMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createOwnerAccount(ownerId, "COMPANY", "Synthetic company");

        assertThat(result.accountType()).isEqualTo("COMPANY");
        assertThat(result.displayName()).isEqualTo("Synthetic company");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        org.mockito.ArgumentCaptor<AccountMemberEntity> member =
                org.mockito.ArgumentCaptor.forClass(AccountMemberEntity.class);
        verify(store).save(member.capture());
        assertThat(member.getValue().getUserPublicId()).isEqualTo(ownerId);
        assertThat(member.getValue().getRoleCode()).isEqualTo("OWNER");
        assertThat(member.getValue().getStatusCode()).isEqualTo("ACTIVE");
        verify(auditRecorder).record(any(AuditRecord.class));
    }

    @Test
    void rejectsUnknownIdentityWithoutCreatingAnAccount() {
        UUID unknownUserId = UUID.randomUUID();
        when(identityUsers.existsById(unknownUserId)).thenReturn(false);

        assertThatThrownBy(() -> service.createOwnerAccount(unknownUserId, "INDIVIDUAL", "Synthetic"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(store, never()).save(any(ClientAccountEntity.class));
        verify(store, never()).save(any(AccountMemberEntity.class));
        verify(auditRecorder, never()).record(any(AuditRecord.class));
    }

    @Test
    void rejectsUnsupportedTypesAndInvalidSearchFilters() {
        UUID ownerId = UUID.randomUUID();
        when(identityUsers.existsById(ownerId)).thenReturn(true);

        assertThatThrownBy(() -> service.createOwnerAccount(ownerId, null, "Synthetic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported account type");
        assertThatThrownBy(() -> service.createOwnerAccount(ownerId, "UNSUPPORTED", "Synthetic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported account type");
        assertThatThrownBy(() -> service.search("x".repeat(121), null, PageQuery.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search text exceeds the allowed length");
        assertThatThrownBy(() -> service.search(null, "pending", PageQuery.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported account status");
        verify(store, never()).save(any(ClientAccountEntity.class));
    }

    @Test
    void normalizesFiltersAndMapsSearchAndOwnershipResults() {
        ClientAccountEntity account = new ClientAccountEntity(UUID.randomUUID(), ClientAccountType.COMPANY,
                "Synthetic company", CREATED_AT);
        when(store.search(org.mockito.ArgumentMatchers.eq("Example"),
                org.mockito.ArgumentMatchers.eq("INACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(account), PageRequest.of(1, 10), 1));
        when(store.findOwnedBy(account.getPublicId())).thenReturn(Optional.of(account));

        var result = service.search("  Example  ", " inactive ", PageQuery.of(1, 10));

        assertThat(result.content()).singleElement()
                .extracting(com.simbio.core.customer.api.CustomerAccountSummary::displayName)
                .isEqualTo("Synthetic company");
        assertThat(service.findOwnedBy(account.getPublicId())).contains(result.content().getFirst());
        assertThat(service.findOwnedBy(UUID.randomUUID())).isEmpty();
        verify(store).search("Example", "INACTIVE", PageRequest.of(1, 10,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
    }
}
