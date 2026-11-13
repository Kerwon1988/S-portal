package com.simbio.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.simbio.core.PortalApplication;

class DocumentationArchitectureTest {

    private static final Pattern INVENTORY_ROW = Pattern.compile("(?m)^\\|\\s*([^|]+?)\\s*\\|");
    private static final Path INVENTORY = Path.of("INVENTARIO.md");

    @Test
    void everyDetectedModulithModuleHasAnInventoryOwner() throws IOException {
        Set<String> modules = ApplicationModules.of(PortalApplication.class).stream()
                .map(module -> module.getIdentifier().toString())
                .collect(Collectors.toSet());
        String inventory = Files.readString(INVENTORY);
        Set<String> listed = INVENTORY_ROW.matcher(inventory).results()
                .map(result -> result.group(1).trim().replace("`", ""))
                .collect(Collectors.toSet());

        assertThat(modules).isNotEmpty();
        assertThat(modules).allSatisfy(module -> {
            if (!listed.contains(module)) {
                throw new IllegalStateException(
                        "Spring Modulith module '" + module + "' has no owner row in INVENTARIO.md");
            }
        });
        assertThat(Files.readString(Path.of("docs/arquitectura/modulos.mmd")))
                .contains("flowchart LR", "shared", "config");
    }

    @Test
    void missingModuleOwnerIsReportedByTheInventoryRule() {
        assertThatThrownBy(() -> requireInventoryOwner("customer", "| config | UI and security |"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("customer");
    }

    @Test
    void documenterCanGenerateTheSpringModulithDiagram() {
        ApplicationModules modules = ApplicationModules.of(PortalApplication.class);
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("target/spring-modulith-docs"))
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
        assertThat(Path.of("target/spring-modulith-docs/components.puml")).exists();
    }

    private static void requireInventoryOwner(String module, String inventory) {
        boolean listed = INVENTORY_ROW.matcher(inventory).results()
                .map(result -> result.group(1).trim())
                .anyMatch(module::equals);
        if (!listed) {
            throw new IllegalStateException(
                    "Spring Modulith module '" + module + "' has no owner row in INVENTARIO.md");
        }
    }
}
