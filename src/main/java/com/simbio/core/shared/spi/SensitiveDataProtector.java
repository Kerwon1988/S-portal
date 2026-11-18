package com.simbio.core.shared.spi;

import java.util.Set;

public interface SensitiveDataProtector {

    String protect(String plaintext);

    String reveal(String protectedValue);

    String fingerprint(String purpose, String plaintext);

    Set<String> fingerprintCandidates(String purpose, String plaintext);
}
