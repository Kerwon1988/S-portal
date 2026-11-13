package com.simbio.core.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.simbio.core.PortalApplication;

class ModularityTests {

    @Test
    void applicationModulesVerify() {
        ApplicationModules.of(PortalApplication.class).verify();
    }
}
