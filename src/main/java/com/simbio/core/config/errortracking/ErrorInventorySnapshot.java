package com.simbio.core.config.errortracking;

import java.util.List;

public record ErrorInventorySnapshot(boolean available, int maxEntries, List<ErrorRecord> errors) {

    public ErrorInventorySnapshot {
        errors = List.copyOf(errors);
    }
}
