package com.simbio.core.config.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AesGcmSensitiveDataProtectorTest {

    @Test
    void encryptsWithVersionedKeyAndAuthenticatesCiphertext() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setCurrentKeyId("test-key");
        properties.setKeys("test-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        AesGcmSensitiveDataProtector protector = new AesGcmSensitiveDataProtector(properties);

        String protectedValue = protector.protect("synthetic-value");

        assertThat(protectedValue).startsWith("v1:test-key:");
        assertThat(protectedValue).doesNotContain("synthetic-value");
        assertThat(protector.reveal(protectedValue)).isEqualTo("synthetic-value");
        int ciphertextStart = protectedValue.lastIndexOf(':') + 1;
        char replacement = protectedValue.charAt(ciphertextStart) == 'A' ? 'B' : 'A';
        String tampered = protectedValue.substring(0, ciphertextStart) + replacement
                + protectedValue.substring(ciphertextStart + 1);
        assertThatThrownBy(() -> protector.reveal(tampered))
                .isInstanceOf(InvalidProtectedDataException.class);
    }

    @Test
    void requiresAnExplicitActiveKeyToEncrypt() {
        AesGcmSensitiveDataProtector protector = new AesGcmSensitiveDataProtector(new EncryptionProperties());

        assertThatThrownBy(() -> protector.protect("synthetic-value"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active encryption key");
    }

    @Test
    void fingerprintsAreKeyedPurposeScopedAndSupportPreviousKeysDuringRotation() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setCurrentKeyId("key-two");
        properties.setKeys(
                "key-one=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=,"
                        + "key-two=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=");
        AesGcmSensitiveDataProtector protector = new AesGcmSensitiveDataProtector(properties);

        String current = protector.fingerprint("invitation-email", "synthetic@example.test");
        Set<String> candidates = protector.fingerprintCandidates(
                "invitation-email", "synthetic@example.test");

        assertThat(current).hasSize(64).doesNotContain("synthetic@example.test");
        assertThat(candidates).hasSize(2).contains(current);
        assertThat(protector.fingerprint("invitation-email", "synthetic@example.test")).isEqualTo(current);
        assertThat(protector.fingerprint("other-purpose", "synthetic@example.test")).isNotEqualTo(current);

        EncryptionProperties previousKeyProperties = new EncryptionProperties();
        previousKeyProperties.setCurrentKeyId("key-one");
        previousKeyProperties.setKeys("key-one=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        AesGcmSensitiveDataProtector previousKeyProtector =
                new AesGcmSensitiveDataProtector(previousKeyProperties);
        assertThat(candidates).contains(
                previousKeyProtector.fingerprint("invitation-email", "synthetic@example.test"));

        EncryptionProperties unrelatedProperties = new EncryptionProperties();
        unrelatedProperties.setCurrentKeyId("key-two");
        unrelatedProperties.setKeys("key-two=AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI=");
        AesGcmSensitiveDataProtector unrelatedProtector =
                new AesGcmSensitiveDataProtector(unrelatedProperties);
        assertThat(unrelatedProtector.fingerprint("invitation-email", "synthetic@example.test"))
                .isNotEqualTo(current);
    }
}
