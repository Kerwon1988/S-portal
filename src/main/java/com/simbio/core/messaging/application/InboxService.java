package com.simbio.core.messaging.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class InboxService {

    private final ConversationStore store;

    public InboxService(ConversationStore store) {
        this.store = Objects.requireNonNull(store);
    }

    @Transactional(readOnly = true)
    public PageResult<InboxConversationSummary> search(
            AuthenticatedUser actor,
            InboxFilters filters,
            PageQuery page) {
        requireInboxPermission(actor);
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(page, "page must not be null");

        boolean canViewRequests = actor.hasPermission(Permission.REQUESTS_VIEW_ALL);
        if (filters.contextType() == ConversationContextType.REQUEST && !canViewRequests) {
            throw new AccessDeniedException("Request conversations are not available to this actor");
        }

        var result = store.searchInbox(
                filters, actor.userId(), canViewRequests, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }

    private static void requireInboxPermission(AuthenticatedUser actor) {
        if (actor == null
                || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)
                || !actor.hasPermission(Permission.INBOX_MANAGE)) {
            throw new AccessDeniedException("Inbox management permission required");
        }
    }
}
