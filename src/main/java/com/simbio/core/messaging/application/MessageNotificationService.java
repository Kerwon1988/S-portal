package com.simbio.core.messaging.application;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageNotificationApi;
import com.simbio.core.messaging.api.MessageNotificationTarget;
import com.simbio.core.messaging.domain.ContactSubmissionEntity;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.shared.spi.SensitiveDataProtector;
import com.simbio.core.shared.value.EmailAddress;

@Service
public class MessageNotificationService implements MessageNotificationApi {

    private final ConversationStore store;
    private final SensitiveDataProtector dataProtector;

    public MessageNotificationService(ConversationStore store, SensitiveDataProtector dataProtector) {
        this.store = store;
        this.dataProtector = dataProtector;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageNotificationTarget> notificationTarget(UUID conversationPublicId) {
        if (conversationPublicId == null) {
            return Optional.empty();
        }
        Optional<ConversationEntity> found = store.findConversation(conversationPublicId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ConversationEntity conversation = found.get();
        if (conversation.getContextType() == ConversationContextType.REQUEST) {
            if (conversation.getOwnerUserId() == null) {
                return Optional.empty();
            }
            return Optional.of(MessageNotificationTarget.toUser(conversation.getOwnerUserId(),
                    "/cuenta/solicitudes/" + conversation.getContextPublicId()));
        }
        Optional<ContactSubmissionEntity> submission = store.findContactSubmission(conversation.getId());
        return submission.map(value -> MessageNotificationTarget.toAddress(
                EmailAddress.of(dataProtector.reveal(value.getEmailEncrypted())),
                Locale.forLanguageTag(value.getLocale())));
    }
}
