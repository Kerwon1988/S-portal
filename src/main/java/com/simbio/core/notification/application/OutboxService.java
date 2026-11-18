package com.simbio.core.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.domain.OutboxMessage;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class OutboxService implements EmailSender {

    private final OutboxStore store;
    private final ObjectMapper objectMapper;
    private final SensitiveDataProtector dataProtector;
    private final Clock clock;

    @Autowired
    public OutboxService(
            OutboxStore store,
            ObjectMapper objectMapper,
            SensitiveDataProtector dataProtector) {
        this(store, objectMapper, dataProtector, Clock.systemUTC());
    }

    OutboxService(
            OutboxStore store,
            ObjectMapper objectMapper,
            SensitiveDataProtector dataProtector,
            Clock clock) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.dataProtector = dataProtector;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UUID enqueue(EmailCommand command) {
        UUID id = UUID.randomUUID();
        try {
            String payload = objectMapper.writeValueAsString(command);
            if (payload.length() > 16_000) {
                throw new IllegalArgumentException("Email payload exceeds the allowed size");
            }
            store.save(new OutboxMessage(id, dataProtector.protect(payload), Instant.now(clock)));
            return id;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize an email command", exception);
        }
    }

    public EmailCommand decrypt(OutboxMessage message) {
        if (message.getPayloadEncrypted() == null) {
            throw new IllegalStateException("Outbox payload has already been purged");
        }
        try {
            return objectMapper.readValue(dataProtector.reveal(message.getPayloadEncrypted()), EmailCommand.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not deserialize an outbox payload", exception);
        }
    }
}
