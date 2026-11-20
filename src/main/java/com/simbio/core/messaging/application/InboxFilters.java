package com.simbio.core.messaging.application;

import java.util.Objects;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationInboxState;

public record InboxFilters(
        ConversationContextType contextType,
        ConversationStatus status,
        ConversationInboxState inboxState,
        InboxAssignmentFilter assignment,
        boolean unreadOnly,
        String searchTerm) {

    public static final int MAX_SEARCH_LENGTH = 120;

    public InboxFilters {
        inboxState = inboxState == null ? ConversationInboxState.ACTIVE : inboxState;
        Objects.requireNonNull(assignment, "assignment filter must not be null");
        searchTerm = searchTerm == null ? "" : searchTerm.strip();
        if (searchTerm.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("Search term is too long");
        }
    }

    public InboxFilters(
            ConversationContextType contextType,
            ConversationStatus status,
            InboxAssignmentFilter assignment,
            boolean unreadOnly,
            String searchTerm) {
        this(contextType, status, ConversationInboxState.ACTIVE, assignment, unreadOnly, searchTerm);
    }

    public boolean assignedToActorOnly() {
        return assignment == InboxAssignmentFilter.MINE;
    }

    public boolean unassignedOnly() {
        return assignment == InboxAssignmentFilter.UNASSIGNED;
    }
}
