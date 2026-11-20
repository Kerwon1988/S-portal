package com.simbio.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.domain.OutboxMessage;
import com.simbio.core.shared.spi.SensitiveDataProtector;
import com.simbio.core.shared.value.EmailAddress;

class OutboxServiceTest {

    @Test
    void encryptsSerializedEmailBeforeSavingOutboxMessage() throws Exception {
        OutboxStore store = mock(OutboxStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.protect(anyString())).thenReturn("protected-payload");
        OutboxService service = new OutboxService(
                store,
                new ObjectMapper(),
                protector,
                Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC));
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH,
                Map.of("url", "https://portal.example.test/verify?token=synthetic"));

        service.enqueue(command);

        ArgumentCaptor<OutboxMessage> saved = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(store).save(saved.capture());
        assertThat(saved.getValue().getPayloadEncrypted()).isEqualTo("protected-payload");
        assertThat(saved.getValue().getPayloadEncrypted()).doesNotContain("synthetic@example.test");
        assertThat(saved.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(Instant.parse("2026-11-18T10:00:00Z"));
        ArgumentCaptor<String> serialized = ArgumentCaptor.forClass(String.class);
        verify(protector).protect(serialized.capture());
        assertThat(serialized.getValue()).contains("synthetic@example.test");
    }

    @Test
    void deterministicKeyPreventsDuplicateEventFromCreatingSecondOutboxMessage() {
        OutboxStore store = mock(OutboxStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.protect(anyString())).thenReturn("protected-payload");
        OutboxService service = new OutboxService(store, new ObjectMapper(), protector,
                Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC));
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"), EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH, Map.of("url", "https://portal.example.test/verify?token=synthetic"));
        UUID id = UUID.nameUUIDFromBytes("REQUEST_CREATED:resource".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(store.findById(id)).thenReturn(Optional.empty(), Optional.of(new OutboxMessage(id, "existing", Instant.now())));

        UUID first = service.enqueueOnce("REQUEST_CREATED:resource", command);
        UUID second = service.enqueueOnce("REQUEST_CREATED:resource", command);

        assertThat(first).isEqualTo(id);
        assertThat(second).isEqualTo(id);
        verify(store).save(org.mockito.ArgumentMatchers.any(OutboxMessage.class));
    }

    @Test
    void rejectsOversizedPayloadBeforeEncryptionOrPersistence() {
        OutboxStore store = mock(OutboxStore.class);
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        OutboxService service = new OutboxService(store, new ObjectMapper(), protector);
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH,
                Map.of(
                        "one", "a".repeat(4096),
                        "two", "b".repeat(4096),
                        "three", "c".repeat(4096),
                        "four", "d".repeat(4096)));

        assertThatThrownBy(() -> service.enqueue(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email payload exceeds the allowed size");
        verify(protector, never()).protect(anyString());
        verify(store, never()).save(org.mockito.ArgumentMatchers.any(OutboxMessage.class));
    }
}
