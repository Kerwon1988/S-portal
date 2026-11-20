package com.simbio.core.simbiosphere.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.simbio.core.simbiosphere.config.SimbioSphereFallbackProperties;

@Controller
public class SimbioSphereFallbackController {
    private final SimbioSphereFallbackProperties properties;

    public SimbioSphereFallbackController(SimbioSphereFallbackProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/continuar/simbiosphere")
    public ResponseEntity<Void> continueExternally() {
        if (!properties.available()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(properties.onboardingUrl().toString()))
                .build();
    }
}
