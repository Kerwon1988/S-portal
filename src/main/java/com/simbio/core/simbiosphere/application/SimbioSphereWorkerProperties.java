package com.simbio.core.simbiosphere.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.simbiosphere.worker")
public class SimbioSphereWorkerProperties {
    private boolean enabled;
    private String workerName = "portal-" + java.lang.ProcessHandle.current().pid();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String workerName() { return workerName; }
}
