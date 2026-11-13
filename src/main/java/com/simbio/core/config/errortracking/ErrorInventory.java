package com.simbio.core.config.errortracking;

import java.util.List;

public record ErrorInventory(int schemaVersion, List<ErrorRecord> errors) {

    public ErrorInventory {
        errors = List.copyOf(errors);
    }
}
