package com.simbio.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PortalApplicationTests {

    @Autowired
    private PortalApplication application;

    @Autowired
    private Environment environment;

    @Test
    void loadsApplicationOnPort8090WithJava21() {
        assertThat(application).isNotNull();
        assertThat(environment.getProperty("server.port")).isEqualTo("8090");
        assertThat(Runtime.version().feature()).isEqualTo(21);
    }
}
