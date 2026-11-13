package com.simbio.core.shared.paging;

public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
