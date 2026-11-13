package com.simbio.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class AssetManifest {

    private final Map<String, String> assets;

    public AssetManifest(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource("static/assets/asset-manifest.json").getInputStream()) {
            assets = Map.copyOf(objectMapper.readValue(input, new TypeReference<Map<String, String>>() {}));
        } catch (IOException exception) {
            throw new IllegalStateException("The fingerprinted frontend asset manifest is missing or invalid", exception);
        }
    }

    public Map<String, String> assets() {
        return assets;
    }
}
