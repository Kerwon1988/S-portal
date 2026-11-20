package com.simbio.core.simbiosphere.application;

public class DemoClientException extends RuntimeException {
    private final int status;
    private final String code;
    private final String correlationId;
    private final boolean retryable;

    public DemoClientException(int status, String code, String correlationId, boolean retryable,
            Throwable cause) {
        super("SimbioSphere request failed (status=" + status + ", code=" + code + ")", cause);
        this.status = status;
        this.code = code == null ? "UNKNOWN" : code;
        this.correlationId = correlationId;
        this.retryable = retryable;
    }

    public int status() { return status; }
    public String code() { return code; }
    public String correlationId() { return correlationId; }
    public boolean retryable() { return retryable; }
}
