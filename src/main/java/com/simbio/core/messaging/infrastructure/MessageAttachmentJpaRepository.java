package com.simbio.core.messaging.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.messaging.domain.MessageAttachmentEntity;

interface MessageAttachmentJpaRepository extends JpaRepository<MessageAttachmentEntity, Long> {

    @Query("""
            select attachment from MessageAttachmentEntity attachment, MessageEntity message
            where attachment.messageId = message.id and message.conversation.id = :conversationId
            order by attachment.createdAt, attachment.id
            """)
    List<MessageAttachmentEntity> findByConversationId(@Param("conversationId") Long conversationId);

    Optional<MessageAttachmentEntity> findByFilePublicId(UUID filePublicId);
}
