package com.simbio.core.identity.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class Totp {

    private static final long PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    private Totp() {
    }

    static long matchingStep(String secret, String code, long currentStep, long lastAcceptedStep) {
        if (code == null || !code.matches("[0-9]{6}")) {
            return -1;
        }
        for (long step = Math.max(0, currentStep - 1); step <= currentStep + 1; step++) {
            if (step > lastAcceptedStep && MessageDigest.isEqual(
                    code.getBytes(StandardCharsets.US_ASCII),
                    codeAt(secret, step).getBytes(StandardCharsets.US_ASCII))) {
                return step;
            }
        }
        return -1;
    }

    static long currentStep(long epochSeconds) {
        return epochSeconds / PERIOD_SECONDS;
    }

    private static String codeAt(String secret, long step) {
        try {
            byte[] key = decodeBase32(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("TOTP cryptographic primitives are unavailable", exception);
        }
    }

    private static byte[] decodeBase32(String encoded) {
        String normalized = encoded.toUpperCase(Locale.ROOT).replace("=", "");
        StringBuilder bits = new StringBuilder(normalized.length() * 5);
        for (char character : normalized.toCharArray()) {
            int value = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid TOTP secret");
            }
            bits.append(String.format(Locale.ROOT, "%5s", Integer.toBinaryString(value)).replace(' ', '0'));
        }
        byte[] decoded = new byte[bits.length() / 8];
        for (int index = 0; index < decoded.length; index++) {
            decoded[index] = (byte) Integer.parseInt(bits.substring(index * 8, index * 8 + 8), 2);
        }
        return decoded;
    }

    static String encodeBase32(byte[] value) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder result = new StringBuilder((value.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte item : value) {
            buffer = (buffer << 8) | (item & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(alphabet[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(alphabet[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return result.toString();
    }

    static String recoveryHash(String code) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime", exception);
        }
    }
}
