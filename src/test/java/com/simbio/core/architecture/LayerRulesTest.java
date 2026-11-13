package com.simbio.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.simbio.core.PortalApplication;
import com.simbio.architecture.fixtures.api.BadApi;
import com.simbio.architecture.fixtures.application.ApplicationPort;
import com.simbio.architecture.fixtures.application.ApplicationService;
import com.simbio.architecture.fixtures.domain.DomainType;
import com.simbio.architecture.fixtures.identity.BusinessType;
import com.simbio.architecture.fixtures.infrastructure.BadRepository;
import com.simbio.architecture.fixtures.infrastructure.GoodAdapter;
import com.simbio.architecture.fixtures.shared.BadShared;
import com.simbio.architecture.fixtures.web.BadController;
import com.simbio.architecture.fixtures.web.EntityInWrongLayer;

class LayerRulesTest {

    private static final ArchRule LAYER_DEPENDENCIES = classes()
            .should(new ArchCondition<>("respect web -> application -> domain layer direction") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String sourcePackage = item.getPackageName();
                    for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                        String targetPackage = dependency.getTargetClass().getPackageName();
                        boolean violation =
                                isPackage(sourcePackage, "web")
                                        && isPackage(targetPackage, "domain", "infrastructure")
                                || isPackage(sourcePackage, "application")
                                        && isPackage(targetPackage, "web", "infrastructure")
                                || isPackage(sourcePackage, "domain")
                                        && isPackage(targetPackage, "web", "application", "infrastructure");
                        if (violation) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }

                private boolean isPackage(String packageName, String... layers) {
                    for (String layer : layers) {
                        if (packageName.endsWith("." + layer) || packageName.contains("." + layer + ".")) {
                            return true;
                        }
                    }
                    return false;
                }
            })
            .allowEmptyShould(true);

    private static final ArchRule INFRASTRUCTURE_IMPLEMENTS_PORTS = classes()
            .that().resideInAPackage("..infrastructure..")
            .should(new ArchCondition<>("implement a port from an application package") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean implementsApplicationPort = item.getAllRawInterfaces().stream()
                            .anyMatch(type -> type.getPackageName().endsWith(".application")
                                    || type.getPackageName().contains(".application."));
                    if (!implementsApplicationPort) {
                        events.add(SimpleConditionEvent.violated(
                                item, item.getName() + " does not implement an application port"));
                    }
                }
            })
            .allowEmptyShould(true);

    private static final ArchRule ENTITIES_IN_DOMAIN = classes()
            .that().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().resideInAPackage("..domain..")
            .allowEmptyShould(true);

    private static final ArchRule CONTROLLERS_DO_NOT_DEPEND_ON_REPOSITORIES = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true);

    private static final ArchRule API_DOES_NOT_DEPEND_ON_WEB_OR_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..web..", "..infrastructure..")
            .allowEmptyShould(true);

    private static final ArchRule SHARED_AND_CONFIG_DO_NOT_DEPEND_ON_BUSINESS = noClasses()
            .that().resideInAnyPackage("..shared..", "..config..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..audit..", "..budget..", "..client..", "..content..", "..files..",
                    "..identity..", "..legal..", "..messaging..", "..notification..",
                    "..request..", "..simbiosphere..", "..staff..")
            .allowEmptyShould(true);

    private static final ArchRule[] RULES = {
        LAYER_DEPENDENCIES,
        INFRASTRUCTURE_IMPLEMENTS_PORTS,
        ENTITIES_IN_DOMAIN,
        CONTROLLERS_DO_NOT_DEPEND_ON_REPOSITORIES,
        API_DOES_NOT_DEPEND_ON_WEB_OR_INFRASTRUCTURE,
        SHARED_AND_CONFIG_DO_NOT_DEPEND_ON_BUSINESS
    };

    @Test
    void applicationClassesRespectAllSixRules() {
        JavaClasses classes = new ClassFileImporter().importPath(java.nio.file.Path.of("target/classes"));
        for (ArchRule rule : RULES) {
            rule.check(classes);
        }
    }

    @Test
    void negativeFixturesFailTheirRulesAndPositiveAdapterPasses() {
        assertRuleFails(LAYER_DEPENDENCIES, BadController.class, DomainType.class);
        assertRuleFails(LAYER_DEPENDENCIES, ApplicationService.class, BadRepository.class);
        assertRuleFails(LAYER_DEPENDENCIES, DomainType.class, BadRepository.class);
        assertRuleFails(INFRASTRUCTURE_IMPLEMENTS_PORTS, BadRepository.class);
        INFRASTRUCTURE_IMPLEMENTS_PORTS.check(importClasses(GoodAdapter.class, ApplicationPort.class));
        assertRuleFails(ENTITIES_IN_DOMAIN, EntityInWrongLayer.class);
        assertRuleFails(CONTROLLERS_DO_NOT_DEPEND_ON_REPOSITORIES, BadController.class, BadRepository.class);
        assertRuleFails(API_DOES_NOT_DEPEND_ON_WEB_OR_INFRASTRUCTURE,
                BadApi.class, BadController.class, BadRepository.class);
        assertRuleFails(SHARED_AND_CONFIG_DO_NOT_DEPEND_ON_BUSINESS, BadShared.class, BusinessType.class);
    }

    private static void assertRuleFails(ArchRule rule, Class<?>... fixtureClasses) {
        assertThatThrownBy(() -> rule.check(importClasses(fixtureClasses)))
                .isInstanceOf(AssertionError.class);
    }

    private static JavaClasses importClasses(Class<?>... fixtureClasses) {
        return new ClassFileImporter().importClasses(fixtureClasses);
    }
}
