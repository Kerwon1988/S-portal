package com.simbio.core.messaging.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.messaging.domain.ContactSubmissionEntity;

interface ContactSubmissionJpaRepository extends JpaRepository<ContactSubmissionEntity, Long> {
    Optional<ContactSubmissionEntity> findByConversationId(Long conversationId);
    Optional<ContactSubmissionEntity> findByPublicId(UUID publicId);
}
