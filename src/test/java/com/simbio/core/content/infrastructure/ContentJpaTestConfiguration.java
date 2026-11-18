package com.simbio.core.content.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(JpaContentStore.class)
public class ContentJpaTestConfiguration {
}
