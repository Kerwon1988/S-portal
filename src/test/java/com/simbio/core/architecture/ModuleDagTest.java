package com.simbio.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.simbio.core.PortalApplication;

class ModuleDagTest {

    @Test
    void moduleDagCsvMatchesDetectedModulesAndHasNoCycles() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/architecture/module-dag.csv"));
        assertThat(lines).isNotEmpty();
        assertThat(lines.get(0)).isEqualTo("module,depends_on");

        Map<String, Set<String>> dependencies = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split(",", -1);
            assertThat(columns).hasSize(2);
            String module = columns[0].trim();
            assertThat(module).isNotBlank();
            Set<String> moduleDependencies = new HashSet<>();
            if (!columns[1].isBlank()) {
                for (String dependency : columns[1].split(";")) {
                    moduleDependencies.add(dependency.trim());
                }
            }
            assertThat(dependencies.putIfAbsent(module, moduleDependencies)).isNull();
        }

        Set<String> discoveredModules = ApplicationModules.of(PortalApplication.class)
                .stream()
                .map(module -> module.getDisplayName().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(dependencies.keySet()).isEqualTo(discoveredModules);
        dependencies.values().forEach(moduleDependencies ->
                assertThat(dependencies.keySet()).containsAll(moduleDependencies));
        assertAcyclic(dependencies);
    }

    private static void assertAcyclic(Map<String, Set<String>> dependencies) {
        Map<String, Integer> indegrees = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        dependencies.keySet().forEach(module -> indegrees.put(module, 0));
        dependencies.forEach((module, moduleDependencies) -> moduleDependencies.forEach(dependency -> {
            indegrees.compute(module, (ignored, count) -> count + 1);
            dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(module);
        }));

        ArrayDeque<String> ready = new ArrayDeque<>();
        indegrees.forEach((module, degree) -> {
            if (degree == 0) {
                ready.add(module);
            }
        });

        int visited = 0;
        while (!ready.isEmpty()) {
            String module = ready.remove();
            visited++;
            for (String dependent : dependents.getOrDefault(module, List.of())) {
                int remaining = indegrees.compute(dependent, (ignored, count) -> count - 1);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        assertThat(visited).isEqualTo(dependencies.size());
    }
}
