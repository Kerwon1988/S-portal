package com.simbio.core.config.crypto;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.encryption")
public class EncryptionProperties {

    private String currentKeyId;
    private String keys;

    public String getCurrentKeyId() {
        return currentKeyId;
    }

    public void setCurrentKeyId(String currentKeyId) {
        this.currentKeyId = currentKeyId;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    byte[] currentKeyMaterial() {
        if (currentKeyId == null || currentKeyId.isBlank() || keys == null || keys.isBlank()) {
            throw new IllegalStateException("An active encryption key is required for ALTCHA signatures");
        }
        for (String item : keys.split(",")) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length == 2 && currentKeyId.equals(pair[0])) {
                byte[] key;
                try {
                    key = Base64.getDecoder().decode(pair[1]);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException("The active encryption key is invalid", exception);
                }
                if (key.length != 32) {
                    throw new IllegalStateException("The active encryption key must contain 256 bits");
                }
                return key;
            }
        }
        throw new IllegalStateException("The active encryption key is not configured");
    }
}
