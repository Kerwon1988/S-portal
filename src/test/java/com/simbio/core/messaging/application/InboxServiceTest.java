package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private ConversationStore store;

    @InjectMocks
    private InboxService service;

    @Test
    void deniesClientAndEmployeeWithoutInboxPermissionBeforeQuerying() {
        AuthenticatedUser client = user(Role.CLIENT);
        AuthenticatedUser employee = user(Role.EMPLOYEE, Permission.REQUESTS_VIEW_ALL);
        InboxFilters filters = filters(null);

        assertThatThrownBy(() -> service.search(client, filters, PageQuery.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.search(employee, filters, PageQuery.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(store);
    }

    @Test
    void refusesRequestFilterWithoutContextPermission() {
        AuthenticatedUser employee = user(Role.EMPLOYEE, Permission.INBOX_MANAGE);
        InboxFilters filters = new InboxFilters(
                ConversationContextType.REQUEST, null, InboxAssignmentFilter.ALL, false, "");

        assertThatThrownBy(() -> service.search(employee, filters, PageQuery.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(store);
    }

    @Test
    void queriesStablePageAndIncludesRequestsOnlyWithExplicitPermission() {
        AuthenticatedUser employee = user(
                Role.EMPLOYEE, Permission.INBOX_MANAGE, Permission.REQUESTS_VIEW_ALL);
        InboxFilters filters = filters("synthetic term");
        InboxConversationSummary summary = new InboxConversationSummary(
                UUID.randomUUID(), ConversationContextType.REQUEST, ConversationStatus.OPEN, null,
                Instant.parse("2026-11-19T14:00:00Z"));
        when(store.searchInbox(eq(filters), eq(employee.userId()), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(1, 20), 21));

        var result = service.search(employee, filters, PageQuery.of(1, 20));

        assertThat(result.content()).containsExactly(summary);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(21);
        verify(store).searchInbox(eq(filters), eq(employee.userId()), eq(true), eq(PageRequest.of(1, 20)));
    }

    @Test
    void rejectsOverlongSearchTermsAndNormalizesWhitespace() {
        assertThat(new InboxFilters(null, null, InboxAssignmentFilter.ALL, false, "  phrase  ").searchTerm())
                .isEqualTo("phrase");
        assertThatThrownBy(() -> new InboxFilters(
                null, null, InboxAssignmentFilter.ALL, false, "x".repeat(InboxFilters.MAX_SEARCH_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static InboxFilters filters(String searchTerm) {
        return new InboxFilters(null, null, InboxAssignmentFilter.MINE, true, searchTerm);
    }

    private static AuthenticatedUser user(Role role, Permission... permissions) {
        return new AuthenticatedUser(UUID.randomUUID(), role, Set.of(permissions));
    }
}
