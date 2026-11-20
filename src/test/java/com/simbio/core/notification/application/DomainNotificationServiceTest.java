package com.simbio.core.notification.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.simbio.core.messaging.api.MessageNotificationApi;
import com.simbio.core.messaging.api.MessageNotificationTarget;
import com.simbio.core.messaging.api.MessagePosted;
import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.api.RequestNotificationRequested;
import com.simbio.core.shared.spi.LocalePreference;
import com.simbio.core.shared.value.EmailAddress;

class DomainNotificationServiceTest {

    @Test
    void messagePostedUsesRecipientLocaleAndTokenFreeInternalPath() {
        MessageNotificationApi messages = mock(MessageNotificationApi.class);
        LocalePreference locales = mock(LocalePreference.class);
        EmailSender emails = mock(EmailSender.class);
        UUID conversationId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        when(messages.notificationTarget(conversationId)).thenReturn(Optional.of(
                MessageNotificationTarget.toUser(recipient, "/cuenta/solicitudes/" + UUID.randomUUID())));
        when(locales.findLocale(recipient)).thenReturn(Optional.of("en"));

        new DomainNotificationService(messages, locales, emails)
                .on(new MessagePosted(conversationId, UUID.randomUUID()));

        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emails).enqueue(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().template())
                .isEqualTo(EmailTemplate.MESSAGE_POSTED);
        org.assertj.core.api.Assertions.assertThat(command.getValue().locale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void contactReceivedAcknowledgesSubmittedAddressWithoutPublishingTokenOrPath() {
        MessageNotificationApi messages = mock(MessageNotificationApi.class);
        EmailSender emails = mock(EmailSender.class);
        UUID conversationId = UUID.randomUUID();
        when(messages.notificationTarget(conversationId)).thenReturn(Optional.of(
                MessageNotificationTarget.toAddress(EmailAddress.of("person@example.test"), Locale.forLanguageTag("es"))));

        new DomainNotificationService(messages, mock(LocalePreference.class), emails)
                .on(new MessageReceived(conversationId, UUID.randomUUID()));

        ArgumentCaptor<EmailCommand> command = ArgumentCaptor.forClass(EmailCommand.class);
        verify(emails).enqueue(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().template())
                .isEqualTo(EmailTemplate.CONTACT_ACKNOWLEDGEMENT);
        org.assertj.core.api.Assertions.assertThat(command.getValue().variables()).isEmpty();
    }

    @Test
    void requestNotificationContractUsesOwnerLocaleAndDeterministicKey() {
        MessageNotificationApi messages = mock(MessageNotificationApi.class);
        LocalePreference locales = mock(LocalePreference.class);
        EmailSender emails = mock(EmailSender.class);
        UUID requestId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        when(locales.findLocale(owner)).thenReturn(Optional.of("en"));
        DomainNotificationService service = new DomainNotificationService(messages, locales, emails);
        RequestNotificationRequested event = new RequestNotificationRequested(
                owner, "/cuenta/solicitudes/" + requestId, EmailTemplate.REQUEST_CREATED,
                Locale.forLanguageTag("es"), "REQUEST_CREATED:" + requestId);

        service.on(event);

        verify(emails).enqueueOnce(
                org.mockito.ArgumentMatchers.eq(event.idempotencyKey()),
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.template() == EmailTemplate.REQUEST_CREATED
                                && command.locale().equals(Locale.ENGLISH)));
    }

    @Test
    void unknownConversationDoesNotGuessRecipient() {
        MessageNotificationApi messages = mock(MessageNotificationApi.class);
        EmailSender emails = mock(EmailSender.class);
        UUID conversationId = UUID.randomUUID();
        when(messages.notificationTarget(conversationId)).thenReturn(Optional.empty());

        new DomainNotificationService(messages, mock(LocalePreference.class), emails)
                .on(new MessagePosted(conversationId, UUID.randomUUID()));

        verify(emails, never()).enqueue(org.mockito.ArgumentMatchers.any());
    }
}
