package com.simbio.core.identity.application;

import java.time.Instant;

public interface BootstrapStateStore {

    boolean wasConsumed();

    void consume(Instant consumedAt);
}
