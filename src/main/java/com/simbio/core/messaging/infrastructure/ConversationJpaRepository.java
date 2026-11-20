package com.simbio.core.messaging.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.application.InboxConversationSummary;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.api.ConversationInboxState;

interface ConversationJpaRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByPublicId(UUID publicId);

    void deleteById(Long id);

    List<ConversationEntity> findByInboxStateAndStateChangedAtLessThanEqualOrderByStateChangedAtAsc(
            ConversationInboxState inboxState, java.time.Instant cutoff, Pageable pageable);

    Optional<ConversationEntity> findByContextTypeAndContextPublicId(
            ConversationContextType contextType,
            UUID contextPublicId);

    @Query(
            value = """
                    select new com.simbio.core.messaging.application.InboxConversationSummary(
                        c.publicId, c.contextType, c.status, c.assignedTo, c.createdAt)
                    from ConversationEntity c
                    where c.inboxState = :inboxState
                      and (:includeRequests = true or c.contextType = :contactType)
                      and (:contextType is null or c.contextType = :contextType)
                      and (:status is null or c.status = :status)
                      and (:assignedToActorOnly = false or c.assignedTo = :actorUserId)
                      and (:unassignedOnly = false or c.assignedTo is null)
                      and (:unreadOnly = false or exists (
                          select unread.id from MessageEntity unread
                          where unread.conversation = c
                            and unread.visibility = :publicVisibility
                            and unread.authorType in :inboundAuthors
                            and unread.readAt is null))
                      and (:searchTerm = '' or exists (
                          select searched.id from MessageEntity searched
                          where searched.conversation = c
                            and searched.visibility = :publicVisibility
                            and locate(:searchTerm, lower(searched.body)) > 0))
                    order by c.createdAt desc, c.id desc
                    """,
            countQuery = """
                    select count(c)
                    from ConversationEntity c
                    where c.inboxState = :inboxState
                      and (:includeRequests = true or c.contextType = :contactType)
                      and (:contextType is null or c.contextType = :contextType)
                      and (:status is null or c.status = :status)
                      and (:assignedToActorOnly = false or c.assignedTo = :actorUserId)
                      and (:unassignedOnly = false or c.assignedTo is null)
                      and (:unreadOnly = false or exists (
                          select unread.id from MessageEntity unread
                          where unread.conversation = c
                            and unread.visibility = :publicVisibility
                            and unread.authorType in :inboundAuthors
                            and unread.readAt is null))
                      and (:searchTerm = '' or exists (
                          select searched.id from MessageEntity searched
                          where searched.conversation = c
                            and searched.visibility = :publicVisibility
                            and locate(:searchTerm, lower(searched.body)) > 0))
                    """)
    Page<InboxConversationSummary> searchInbox(
            @Param("inboxState") ConversationInboxState inboxState,
            @Param("contextType") ConversationContextType contextType,
            @Param("status") ConversationStatus status,
            @Param("assignedToActorOnly") boolean assignedToActorOnly,
            @Param("unassignedOnly") boolean unassignedOnly,
            @Param("actorUserId") UUID actorUserId,
            @Param("includeRequests") boolean includeRequests,
            @Param("contactType") ConversationContextType contactType,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("searchTerm") String searchTerm,
            @Param("publicVisibility") MessageVisibility publicVisibility,
            @Param("inboundAuthors") List<MessageAuthorType> inboundAuthors,
            Pageable pageable);
}
