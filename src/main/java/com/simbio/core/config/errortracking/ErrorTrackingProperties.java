package com.simbio.core.config.errortracking;

import java.nio.file.Path;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "portal.error-tracking")
public record ErrorTrackingProperties(
        @NotNull Path inventoryPath,
        @Min(1) @Max(10000) int maxEntries,
        @Min(1024) @Max(52428800) long maxFileBytes,
        @Min(128) @Max(8192) int maxFieldLength,
        @NotBlank @Size(max = 128) String release) {
}
