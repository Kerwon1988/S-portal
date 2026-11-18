package com.simbio.core.config.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.simbio.core.shared.spi.SensitiveDataProtector;

@Component
public class AesGcmSensitiveDataProtector implements SensitiveDataProtector {

    private static final String VERSION = "v1";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final Map<String, byte[]> keys;
    private final String currentKeyId;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSensitiveDataProtector(EncryptionProperties properties) {
        currentKeyId = properties.getCurrentKeyId();
        keys = parseKeys(properties.getKeys());
        if (currentKeyId != null && !currentKeyId.isBlank() && !keys.containsKey(currentKeyId)) {
            throw new IllegalStateException("The active encryption key is not configured");
        }
    }

    @Override
    public String protect(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null");
        }
        byte[] key = activeKey();
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD((VERSION + ":" + currentKeyId).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + currentKeyId + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not protect sensitive data", exception);
        }
    }

    @Override
    public String reveal(String protectedValue) {
        if (protectedValue == null) {
            throw new IllegalArgumentException("Protected value must not be null");
        }
        String[] parts = protectedValue.split(":", -1);
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new InvalidProtectedDataException();
        }
        byte[] key = keys.get(parts[1]);
        if (key == null) {
            throw new IllegalStateException("The encryption key required to reveal data is unavailable");
        }
        try {
            byte[] nonce;
            byte[] ciphertext;
            try {
                nonce = Base64.getUrlDecoder().decode(parts[2]);
                ciphertext = Base64.getUrlDecoder().decode(parts[3]);
            } catch (IllegalArgumentException exception) {
                throw new InvalidProtectedDataException(exception);
            }
            if (nonce.length != NONCE_BYTES) {
                throw new InvalidProtectedDataException();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD((parts[0] + ":" + parts[1]).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new InvalidProtectedDataException(exception);
        }
    }

    @Override
    public String fingerprint(String purpose, String plaintext) {
        validateFingerprintInput(purpose, plaintext);
        return fingerprint(activeKey(), purpose, plaintext);
    }

    @Override
    public Set<String> fingerprintCandidates(String purpose, String plaintext) {
        validateFingerprintInput(purpose, plaintext);
        if (keys.isEmpty()) {
            throw new IllegalStateException("No encryption key is configured for sensitive-data fingerprints");
        }
        return keys.values().stream()
                .map(key -> fingerprint(key, purpose, plaintext))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private byte[] activeKey() {
        if (currentKeyId == null || currentKeyId.isBlank()) {
            throw new IllegalStateException("No active encryption key is configured");
        }
        byte[] key = keys.get(currentKeyId);
        if (key == null) {
            throw new IllegalStateException("No active encryption key is configured");
        }
        return key;
    }

    private static String fingerprint(byte[] encryptionKey, String purpose, String plaintext) {
        try {
            Mac derivation = Mac.getInstance("HmacSHA256");
            derivation.init(new SecretKeySpec(encryptionKey, "HmacSHA256"));
            byte[] fingerprintKey = derivation.doFinal(
                    "s-portal:sensitive-fingerprint:v1".getBytes(StandardCharsets.UTF_8));

            Mac fingerprint = Mac.getInstance("HmacSHA256");
            fingerprint.init(new SecretKeySpec(fingerprintKey, "HmacSHA256"));
            fingerprint.update(purpose.getBytes(StandardCharsets.UTF_8));
            fingerprint.update((byte) 0);
            return java.util.HexFormat.of().formatHex(fingerprint.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not fingerprint sensitive data", exception);
        }
    }

    private static void validateFingerprintInput(String purpose, String plaintext) {
        if (purpose == null || purpose.isBlank() || purpose.length() > 64) {
            throw new IllegalArgumentException("Fingerprint purpose must contain 1 to 64 characters");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("Fingerprint input must not be null");
        }
    }

    private static Map<String, byte[]> parseKeys(String configuredKeys) {
        Map<String, byte[]> parsed = new HashMap<>();
        if (configuredKeys == null || configuredKeys.isBlank()) {
            return Map.of();
        }
        for (String item : configuredKeys.split(",")) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank()) {
                throw new IllegalArgumentException("Encryption keys must use key-id=base64 format");
            }
            byte[] key;
            try {
                key = Base64.getDecoder().decode(pair[1]);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Encryption key material must be Base64", exception);
            }
            if (key.length != 32 || parsed.putIfAbsent(pair[0], key) != null) {
                throw new IllegalArgumentException("Encryption keys must be unique AES-256 keys");
            }
        }
        return Map.copyOf(parsed);
    }
}
