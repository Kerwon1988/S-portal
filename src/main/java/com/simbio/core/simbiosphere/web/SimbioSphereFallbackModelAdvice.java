package com.simbio.core.simbiosphere.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.simbio.core.simbiosphere.config.SimbioSphereFallbackProperties;

@ControllerAdvice
public class SimbioSphereFallbackModelAdvice {
    private final SimbioSphereFallbackProperties properties;

    public SimbioSphereFallbackModelAdvice(SimbioSphereFallbackProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute("simbioSphereFallbackAvailable")
    public boolean fallbackAvailable() {
        return properties.available();
    }
}
