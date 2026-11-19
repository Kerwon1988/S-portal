package com.simbio.core.messaging.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ConversationEntity;

interface ConversationJpaRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByPublicId(UUID publicId);

    Optional<ConversationEntity> findByContextTypeAndContextPublicId(
            ConversationContextType contextType,
            UUID contextPublicId);
}
