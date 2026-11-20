package com.simbio.core.notification.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.value.EmailAddress;

class EmailCommandSecurityTest {

    @Test
    void rejectsTokensAndExternalUrlsFromOperationalTemplates() {
        assertThatThrownBy(() -> EmailCommand.toUser(UUID.randomUUID(), EmailTemplate.MESSAGE_POSTED,
                Locale.ENGLISH, Map.of("url", "/account?token=synthetic")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operational email requires a token-free internal path");
        assertThatThrownBy(() -> EmailCommand.toUser(UUID.randomUUID(), EmailTemplate.PROVISIONING_FAILED,
                Locale.ENGLISH, Map.of("url", "https://external.example.test/alert")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresSpiResolvedUserForOptionalOperationalEmail() {
        assertThatThrownBy(() -> EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"), EmailTemplate.REQUEST_STATUS_CHANGED,
                Locale.ENGLISH, Map.of("url", "/cuenta/solicitudes/abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operational email requires an authorized user recipient");
    }
}
