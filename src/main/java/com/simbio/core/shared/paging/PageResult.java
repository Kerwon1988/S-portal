package com.simbio.core.shared.paging;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1 || size > 100 || content.size() > size) {
            throw new IllegalArgumentException("Invalid page size or content length");
        }
        if (totalElements < 0 || totalElements < content.size()) {
            throw new IllegalArgumentException("Total elements must not be less than page content");
        }
    }
}
