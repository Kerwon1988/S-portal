package com.simbio.core.messaging.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.domain.MessageEntity;

interface MessageJpaRepository extends JpaRepository<MessageEntity, Long> {

    java.util.Optional<MessageEntity> findByPublicId(java.util.UUID publicId);

    @Query("""
            select message from MessageEntity message
            where message.conversation.id = :conversationId
              and (:includeInternal = true or message.visibility = com.simbio.core.messaging.api.MessageVisibility.PUBLIC)
            order by message.createdAt, message.id
            """)
    List<MessageEntity> findForConversation(
            @Param("conversationId") Long conversationId,
            @Param("includeInternal") boolean includeInternal);

    void deleteByConversationId(Long conversationId);
}
