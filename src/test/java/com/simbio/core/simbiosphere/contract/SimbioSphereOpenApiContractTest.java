package com.simbio.core.simbiosphere.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class SimbioSphereOpenApiContractTest {

    private static final Path CONTRACT = Path.of("docs", "plan", "contracts", "simbiosphere-platform-v1.yaml");
    private static final Path WIREMOCK_MAPPINGS =
            Path.of("src", "test", "resources", "wiremock", "simbiosphere", "mappings");

    @Test
    void openApiDefinesSecureProvisioningAndProblemResponsesWithoutPasswordFields() throws IOException {
        Object parsed = new Yaml().load(Files.readString(CONTRACT));
        assertThat(parsed).isInstanceOf(Map.class);
        Map<?, ?> specification = (Map<?, ?>) parsed;

        assertThat(specification.get("openapi")).isEqualTo("3.1.0");
        assertThat(containsKey(specification, "password")).isFalse();
        assertThat(mapAt(specification, "components", "securitySchemes").keySet()
                .containsAll(List.of("PlatformKeyId", "PlatformTimestamp", "PlatformSignature"))).isTrue();
        assertThat(mapAt(specification, "components", "securitySchemes", "PlatformSignature")
                .get("name")).isEqualTo("X-Platform-Signature");

        Map<?, ?> operations = mapAt(specification, "paths", "/api/platform/v1/owner-onboardings", "post");
        assertThat(mapAt(operations, "responses").keySet()
                .containsAll(List.of("201", "409", "422", "429", "503"))).isTrue();
        Map<?, ?> required = mapAt(specification, "components", "schemas", "OwnerOnboardingRequest");
        assertThat(required.get("required")).asList().contains("externalReference");
        assertThat(mapAt(specification, "components", "schemas", "OwnerOnboardingRequest",
                "properties", "externalReference").get("required")).asList()
                .contains("externalAccountId");
        assertThat(mapAt(specification, "components", "schemas", "Problem").get("required")).asList()
                .contains("type", "title", "status", "code");
    }

    @Test
    void wireMockMappingsAreValidJsonAndMatchDocumentedRoutes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (Stream<Path> files = Files.list(WIREMOCK_MAPPINGS)) {
            List<JsonNode> mappings = files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> readMapping(mapper, path))
                    .toList();

            assertThat(mappings).hasSize(3);
            assertThat(mappings).allSatisfy(mapping -> {
                assertThat(mapping.at("/request/method").asText()).isIn("GET", "POST");
                assertThat(mapping.at("/request/headers/X-Platform-Key-Id").isMissingNode()).isFalse();
                assertThat(mapping.at("/request/headers/X-Platform-Signature").isMissingNode()).isFalse();
                assertThat(mapping.at("/response/headers/Cache-Control").asText()).isEqualTo("no-store");
            });
        }
    }

    private static JsonNode readMapping(ObjectMapper mapper, Path path) {
        try {
            return mapper.readTree(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid WireMock mapping " + path.getFileName(), exception);
        }
    }

    private static Map<?, ?> mapAt(Map<?, ?> root, String... path) {
        Object value = root;
        for (String segment : path) {
            assertThat(value).as("OpenAPI path segment %s", segment).isInstanceOf(Map.class);
            value = ((Map<?, ?>) value).get(segment);
        }
        assertThat(value).isInstanceOf(Map.class);
        return (Map<?, ?>) value;
    }

    private static boolean containsKey(Object value, String expectedKey) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().anyMatch(entry ->
                    expectedKey.equalsIgnoreCase(String.valueOf(entry.getKey()))
                            || containsKey(entry.getValue(), expectedKey));
        }
        if (value instanceof Iterable<?> values) {
            for (Object element : values) {
                if (containsKey(element, expectedKey)) {
                    return true;
                }
            }
        }
        return false;
    }
}
