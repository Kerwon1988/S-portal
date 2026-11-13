package com.simbio.core.shared.spi;

import java.util.Map;

@FunctionalInterface
public interface PersonalDataExport {

    void addSection(String name, Map<String, String> values);
}
