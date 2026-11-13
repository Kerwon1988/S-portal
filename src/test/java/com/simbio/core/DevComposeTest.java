package com.simbio.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DevComposeTest {

    @Test
    void composeUsesLocalPortsHealthchecksAndEnvironmentSecret() throws IOException {
        String composeText = Files.readString(Path.of("docker-compose.dev.yml"));
        Map<?, ?> compose = new Yaml().load(composeText);
        Map<?, ?> services = asMap(compose.get("services"));

        assertService(services, "sqlserver", "mcr.microsoft.com/mssql/server:2022-latest",
                "127.0.0.1:${MSSQL_PORT:-14330}:1433");
        assertService(services, "mailpit", "axllent/mailpit:v1.27.8",
                "127.0.0.1:${MAILPIT_SMTP_PORT:-1026}:1025",
                "127.0.0.1:${MAILPIT_UI_PORT:-8026}:8025");
        assertService(services, "clamav", "clamav/clamav:1.4",
                "127.0.0.1:${CLAMAV_PORT:-3310}:3310");

        Map<?, ?> sqlEnvironment = asMap(asMap(services.get("sqlserver")).get("environment"));
        assertThat(sqlEnvironment.get("MSSQL_SA_PASSWORD"))
                .isEqualTo("${MSSQL_SA_PASSWORD:?Set MSSQL_SA_PASSWORD in your local environment}");
        assertThat(Files.readString(Path.of(".env.example")))
                .contains("MSSQL_SA_PASSWORD=")
                .doesNotContain("MSSQL_SA_PASSWORD=Password");
    }

    private static void assertService(
            Map<?, ?> services, String name, String expectedImage, String... expectedPorts) {
        Map<?, ?> service = asMap(services.get(name));
        assertThat(service.get("image")).isEqualTo(expectedImage);
        assertThat(service.get("ports")).isEqualTo(List.of(expectedPorts));
        assertThat(service.containsKey("healthcheck")).isTrue();
    }

    private static Map<?, ?> asMap(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<?, ?>) value;
    }
}
