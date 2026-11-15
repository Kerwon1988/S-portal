package com.simbio.core.audit.application;

import java.util.List;

public record AuditPage(List<AuditView> entries, int page, int totalPages, long totalElements) {

    public AuditPage {
        entries = List.copyOf(entries);
    }
}
