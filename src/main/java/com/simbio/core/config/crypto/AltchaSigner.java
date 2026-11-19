package com.simbio.core.config.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class AltchaSigner {

    private static final byte[] KEY_DERIVATION_LABEL =
            "sportal:altcha:signature:v1".getBytes(StandardCharsets.UTF_8);
    private final EncryptionProperties encryptionProperties;

    public AltchaSigner(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
    }

    public String sign(String challenge) {
        return HexFormat.of().formatHex(signature(challenge));
    }

    public boolean verifies(String challenge, String suppliedSignature) {
        if (challenge == null || suppliedSignature == null || !suppliedSignature.matches("[0-9a-f]{64}")) {
            return false;
        }
        byte[] expected = signature(challenge);
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(suppliedSignature);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return MessageDigest.isEqual(expected, supplied);
    }

    private byte[] signature(String challenge) {
        if (challenge == null || challenge.isBlank()) {
            throw new IllegalArgumentException("ALTCHA challenge is required");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(encryptionProperties.currentKeyMaterial(), "HmacSHA256"));
            byte[] signingKey = mac.doFinal(KEY_DERIVATION_LABEL);
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(challenge.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is required by the runtime", exception);
        }
    }
}
