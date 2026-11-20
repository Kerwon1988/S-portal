package com.simbio.core.simbiosphere.application;

public interface OutboundCircuitBreaker {
    boolean allowRequest();
    void recordSuccess();
    void recordFailure();
}
