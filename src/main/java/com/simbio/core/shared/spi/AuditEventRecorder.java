package com.simbio.core.shared.spi;

public interface AuditEventRecorder {

    void record(AuditEvent event);
}
